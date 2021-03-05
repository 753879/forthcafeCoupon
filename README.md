# forthcafe
# 서비스 시나리오
### 기능적 요구사항
1. 주문내역이 배송되면 쿠폰을 저장한다.
2. 배송이 취소되면 쿠폰도 취소된다.



### 비기능적 요구사항
1. 트랜젝션
   1. 주문내역이 배송되면 쿠폰을 저장한다. → Sync 호출
2. 장애격리
   1. 쿠폰에서 장애가 발송해도 주문취소는 24시간 받을 수 있어야 한다 → Async(event-driven), Eventual Consistency
   1. 쿠폰서비스가 과중되면 쿠폰저장을 잠시 후에 하도록 유도한다 → Circuit breaker, fallback
3. 성능
   1. 고객이 쿠폰내역을 화면에서 확인할 수 있어야 한다 → CQRS

# Event Storming 결과

![EventStormingV1](https://github.com/753879/forthcafeCoupon/blob/main/images/stomming.png)

# 헥사고날 아키텍처 다이어그램 도출
![증빙10](https://github.com/bigot93/forthcafe/blob/main/images/%ED%97%A5%EC%82%AC%EA%B3%A0%EB%82%A0.png)

# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8084, 8088 이다)
```
cd Order
mvn spring-boot:run  

cd Pay
mvn spring-boot:run

cd Delivery
mvn spring-boot:run 

cd MyPage
mvn spring-boot:run 

cd Coupon
mvn spring-boot:run 

cd gateway
mvn spring-boot:run 
```

## DDD 의 적용
msaez.io를 통해 구현한 Aggregate 단위로 Entity를 선언 후, 구현을 진행하였다.
Entity Pattern과 Repository Pattern을 적용하기 위해 Spring Data REST의 RestRepository를 적용하였다.

**Coupon 서비스의 Coupon.java**
```java 
package forthcafe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;


import java.util.List;

@Entity
@Table(name="Coupon_table")
public class Coupon {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String ordererName;
    private String menuName;
    private Long menuId;
    private Double price;
    private Integer quantity;
    private String status;

    @PrePersist
    public void onPrePersist(){
        CouponSaved couponSaved = new CouponSaved();
        BeanUtils.copyProperties(this, couponSaved);
        couponSaved.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        CouponCancelled couponCancelled = new CouponCancelled();
        BeanUtils.copyProperties(this, couponCancelled);
        couponCancelled.publishAfterCommit();

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getOrdererName() {
        return ordererName;
    }

    public void setOrdererName(String ordererName) {
        this.ordererName = ordererName;
    }
    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }
    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
```

**Coupon 서비스의 PolicyHandler.java**
```java
package forthcafe;

import forthcafe.config.kafka.KafkaProcessor;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{

    @Autowired
    CouponRepository couponRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverDeliveryCancelled_CouponCancel(@Payload DeliveryCancelled deliveryCancelled){

        if(deliveryCancelled.isMe()){
            System.out.println("##### listener  : " + deliveryCancelled.toJson());

            Coupon coupon = new Coupon();
            coupon.setId(deliveryCancelled.getId());
            coupon.setMenuId(deliveryCancelled.getMenuId());
            coupon.setMenuName(deliveryCancelled.getMenuName());
            coupon.setOrdererName(deliveryCancelled.getOrdererName());
            coupon.setPrice(deliveryCancelled.getPrice());
            coupon.setQuantity(deliveryCancelled.getQuantity());
            coupon.setStatus("CouponCancelled");

            couponRepository.save(coupon);


        }
    }

}
```

DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

- 주문 후 쿠폰 결과

![image](https://user-images.githubusercontent.com/78134499/109924305-694d9c80-7d03-11eb-8342-50453b4326f0.png)


# GateWay 적용
API GateWay를 통하여 마이크로 서비스들의 집입점을 통일할 수 있다. 다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/** 
        - id: Pay
          uri: http://localhost:8082
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
        - id: Coupon
          uri: http://localhost:8085
          predicates:
            - Path=/coupons/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Order
          uri: http://Order:8080
          predicates:
            - Path=/orders/** 
        - id: Pay
          uri: http://Pay:8080
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://MyPage:8080
          predicates:
            - Path= /myPages/**
        - id: Coupon
          uri: http://Coupon:8080
          predicates:
            - Path=/coupons/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

8088 port로 Coupon서비스 정상 호출

![image](https://user-images.githubusercontent.com/78134499/109924486-b3368280-7d03-11eb-811d-aa4304aa6b86.png)

# CQRS/saga/correlation

Materialized View를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다. 
본 프로젝트에서 View 역할은 MyPages 서비스가 수행한다.

주문(ordered) 실행 후 MyPages 화면

![image](https://user-images.githubusercontent.com/78134499/109927702-f397ff80-7d07-11eb-824e-54c877c0101d.png)

주문(OrderCancelled) 취소 후 MyPages 화면

![image](https://user-images.githubusercontent.com/78134499/109926958-e7f80900-7d06-11eb-852d-272e2df73ec1.png)

위와 같이 주문을 하게되면 Order > Pay > Delivery > Coupon > MyPage로 주문이 Assigned 되고

주문 취소가 되면 Status가 couponCancelled로 Update 되는 것을 볼 수 있다.

또한 Correlation을 Key를 활용하여 Id를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

# 폴리글랏
Order 서비스의 DB와 MyPage의 DB를 다른 DB를 사용하여 폴리글랏을 만족시키고 있다.

**Coupon의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/78134499/109928507-df083700-7d08-11eb-9642-739a58aa4f59.png)

**MyPage의 pom.xml DB 설정 코드**

![image](https://user-images.githubusercontent.com/78134499/109928546-e7607200-7d08-11eb-81fd-7be2377ee7af.png)

# 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 배송(Delivery)과 쿠폰(Coupon) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출하도록 한다.

**Delivery 서비스 내 external.CouponService**
```java
package forthcafe.external;

import org.springframework.cloud.openfeign.FeignClient; 
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Coupon", url="${api.url.coupon}", fallback = CouponServiceImpl.class)
public interface CouponService {

    @RequestMapping(method = RequestMethod.POST, path = "/coupons", consumes = "application/json")
    public void coupon(@RequestBody Coupon coupon);

}

package forthcafe.external;

import org.springframework.stereotype.Service;

@Service
public class CouponServiceImpl implements CouponService {

    // fallback message
    @Override
    public void coupon(Coupon coupon) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!! Coupon service is BUSY !!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!   Try again later   !!!!!!!!!!!!!!!!!!!!!");
    }

}
```

**동작 확인**

잠시 Coupon 서비스 중지
![image](https://user-images.githubusercontent.com/78134499/109933539-dfa3cc00-7d0e-11eb-813c-f9dd900597e2.png)

주문 요청
![image](https://user-images.githubusercontent.com/78134499/109933598-f4805f80-7d0e-11eb-8e44-05bf36d4175d.png)

Delivery 서비스 변화 없음
![image](https://user-images.githubusercontent.com/78134499/109933647-019d4e80-7d0f-11eb-8711-33c8a34b9cc2.png)

Delivery 서비스 재기동 
![image](https://user-images.githubusercontent.com/78134499/109933669-0a8e2000-7d0f-11eb-9425-bdd6f7d258f5.png)

Delivery 서비스에 정상 요청이 됨
![image](https://user-images.githubusercontent.com/78134499/109933712-137ef180-7d0f-11eb-94bd-0621692435e9.png)

Fallback 설정

![image](https://user-images.githubusercontent.com/78134499/109938754-bb96b980-7d13-11eb-8be6-842fca180a84.png)

Fallback 결과(Pay service 종료 후 Order 추가 시) -----확인필요
![image](https://user-images.githubusercontent.com/5147735/109755716-dab91c80-7c29-11eb-9099-ba585115a2a6.png)

# 운영

## CI/CD
* 카프카 설치
```
- 헬름 설치
참고 : http://msaschool.io/operation/implementation/implementation-seven/
curl https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 > get_helm.sh
chmod 700 get_helm.sh
./get_helm.sh

- Azure Only
kubectl patch storageclass managed -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

- 카프카 설치
kubectl --namespace kube-system create sa tiller      # helm 의 설치관리자를 위한 시스템 사용자 생성
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller

helm repo add incubator https://charts.helm.sh/incubator
helm repo update
kubectl create ns kafka
helm install my-kafka --namespace kafka incubator/kafka

kubectl get po -n kafka -o wide
```
* Topic 생성
```
kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --topic forthcafe --create --partitions 1 --replication-factor 1
```
* Topic 확인
```
kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --list
```
* 이벤트 발행하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-producer --broker-list my-kafka:9092 --topic forthcafe
```
* 이벤트 수신하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-consumer --bootstrap-server my-kafka:9092 --topic forthcafe --from-beginning
```

* 소스 가져오기
```
git clone https://github.com/bigot93/forthcafe.git
```

## ConfigMap
* deployment.yml 파일에 설정
```
env:
   - name: SYS_MODE
     valueFrom:
       configMapKeyRef:
         name: systemmode
         key: sysmode
```

* Coupon.java 파일에 설정
```
    @PrePersist
    public void onPrePersist(){
                // configMap 설정
        String sysEnv = System.getenv("SYS_MODE");
        if(sysEnv == null) sysEnv = "LOCAL";
        System.out.println("################## SYSTEM MODE: " + sysEnv);

        CouponSaved couponSaved = new CouponSaved();
        BeanUtils.copyProperties(this, couponSaved);
        couponSaved.publishAfterCommit();
```

* Configmap 생성, 정보 확인
```
kubectl create configmap systemmode --from-literal=sysmode=PRODUCT
kubectl get configmap systemmode -o yaml
```

![image](https://user-images.githubusercontent.com/78134499/109965068-cfe9af00-7d31-11eb-841a-6cf4317ce8c6.png)


* order 1건 추가후 로그 확인
```
kubectl logs {pod ID} 
```
![image](https://user-images.githubusercontent.com/78134499/110054410-618f0600-7d9e-11eb-90d2-bbf4ddb33688.png)



## Deploy / Pipeline

* build 하기
```
cd /forthcafe

cd Order
mvn package 

cd ..
cd Pay
mvn package

cd ..
cd Delivery
mvn package

cd ..
cd gateway
mvn package

cd ..
cd MyPage
mvn package
```

* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법1 : yml파일 이용한 deploy)
```
cd .. 
cd Order
az acr build --registry skuser03 --image skuser03.azurecr.io/order:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy order --type=ClusterIP --port=8080

cd .. 
cd Pay
az acr build --registry skuser03 --image skuser03.azurecr.io/pay:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy pay --type=ClusterIP --port=8080

cd .. 
cd Delivery
az acr build --registry skuser03 --image skuser03.azurecr.io/delivery:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy delivery --type=ClusterIP --port=8080

cd .. 
cd Coupon
az acr build --registry skuser03 --image skuser03.azurecr.io/coupon:v2 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy coupon --type=ClusterIP --port=8080


cd .. 
cd MyPage
az acr build --registry skuser03 --image skuser03.azurecr.io/mypage:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy mypage --type=ClusterIP --port=8080

cd .. 
cd gateway
az acr build --registry skuser03 --image skuser03.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skuser03.azurecr.io/gateway:v1
kubectl expose deploy gateway --type=LoadBalancer --port=8080
```


* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법2)
```
cd ..
cd Order
az acr build --registry skuser03 --image skuser03.azurecr.io/order:v1 .
kubectl create deploy order --image=skuser03.azurecr.io/order:v1
kubectl expose deploy order --type=ClusterIP --port=8080

cd .. 
cd Pay
az acr build --registry skuser03 --image skuser03.azurecr.io/pay:v1 .
kubectl create deploy pay --image=skuser03.azurecr.io/pay:v1
kubectl expose deploy pay --type=ClusterIP --port=8080


cd .. 
cd Delivery
az acr build --registry skuser03 --image skuser03.azurecr.io/delivery:v1 .
kubectl create deploy delivery --image=skuser03.azurecr.io/delivery:v1
kubectl expose deploy delivery --type=ClusterIP --port=8080

cd .. 
cd Coupon
az acr build --registry skuser03 --image skuser03.azurecr.io/coupon:v1 .
kubectl create deploy mypage --image=skuser03.azurecr.io/coupon:v1
kubectl expose deploy mypage --type=ClusterIP --port=8080

cd .. 
cd gateway
az acr build --registry skuser03 --image skuser03.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skuser03.azurecr.io/gateway:v1
kubectl expose deploy gateway --type=LoadBalancer --port=8080

cd .. 
cd MyPage
az acr build --registry skuser03 --image skuser03.azurecr.io/mypage:v1 .
kubectl create deploy mypage --image=skuser03.azurecr.io/mypage:v1
kubectl expose deploy mypage --type=ClusterIP --port=8080

kubectl logs {pod명}
```
* Service, Pod, Deploy 상태 확인

![image](https://user-images.githubusercontent.com/78134499/109949528-cf93e880-7d1e-11eb-8068-b75dd4e53c42.png)

* deployment.yml  참고
```
1. image 설정
2. env 설정 (config Map) 
3. readiness 설정 (무정지 배포)
4. liveness 설정 (self-healing)
5. resource 설정 (autoscaling)
```
![image](https://user-images.githubusercontent.com/78134499/109967620-fd842780-7d34-11eb-9e26-0d7e97464f9f.png)


## 서킷 브레이킹
* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
* Delivery -> Coupon 와의 Req/Res 연결에서 요청이 과도한 경우 CirCuit Breaker 통한 격리
* Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정


Delivery application.yml
```
feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

Coupon 서비스 Coupon.java

```
    @PrePersist
    public void onPrePersist(){
        CouponSaved couponSaved = new CouponSaved();
        BeanUtils.copyProperties(this, couponSaved);
        couponSaved.publishAfterCommit();

        try {
                Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
                e.printStackTrace();
        }

    }
```

* /home/project/team/forthcafe/yaml/siege.yaml
```
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
cd /home/project/personal/forthcafeCoupon/yaml
kubectl apply -f siege.yaml
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 40명 30초 동안 실시
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c30 -t20S  -v --content-type "application/json" 'http://Delivery:8080/deliveries POST {"memuId":2, "quantity":1}'
```
![image](https://user-images.githubusercontent.com/78134499/110056919-fac01b80-7da2-11eb-9529-f6097ca3c2fe.png)


## 오토스케일 아웃
* 앞서 서킷 브레이커(CB) 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

* coupon 서비스 deployment.yml 설정
```
 resources:
            limits:
              cpu: 500m
            requests:
              cpu: 200m
```
* 다시 배포해준다.
```
/home/project/team/forthcafe/Coupon/mvn package
az acr build --registry skuser03 --image skuser03.azurecr.io/coupon:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl expose deploy coupon --type=ClusterIP --port=8080
```

* Coupon 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15프로를 넘어서면 replica 를 5개까지 늘려준다

```
kubectl autoscale deploy coupon --min=1 --max=5 --cpu-percent=15
```

* /home/project/team/forthcafe/yaml/siege.yaml
```
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
/home/project/team/forthcafe/yaml/kubectl apply -f siege.yaml
```

* siege를 활용해서 워크로드를 1000명, 1분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c300 -t60S  -v --content-type "application/json" 'http://10.0.118.176:8080/coupons POST {"memuId":2, "quantity":1}'
```

* 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get deploy coupon -w
```
![image](https://user-images.githubusercontent.com/78134499/109976833-8b651000-7d3f-11eb-8212-d123912cf6f1.png)

```
kubectl get pod
```
![image](https://user-images.githubusercontent.com/78134499/109976858-90c25a80-7d3f-11eb-8267-eabbd70227f3.png)





## 무정지 재배포 (Readiness Probe)

* coupon서비스의 deployment.yml 설정
```
spec:
  replicas: 1
  selector:
    matchLabels:
      app: coupon
  template:
    metadata:
      labels:
        app: coupon
    spec:
      containers:
        - name: coupon
          image: skuser03.azurecr.io/coupon:v4
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8089
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
 ```
 
* 배포진행

![image](https://user-images.githubusercontent.com/78134499/110055629-ac118200-7da0-11eb-9304-69c8b42ee00e.png)


* Siege 결과 Availability가 100% 확인

![image](https://user-images.githubusercontent.com/78134499/110055368-30173a00-7da0-11eb-99c8-77c08fe8b693.png)



## Self-healing (Liveness Probe)
* coupon 서비스 deployment.yml   livenessProbe 설정을 port 8089로 변경 후 배포 하여 liveness probe 가 동작함을 확인 
```
    livenessProbe:
      httpGet:
        path: '/actuator/health'
        port: 8089
      initialDelaySeconds: 5
      periodSeconds: 5
```
![image](https://user-images.githubusercontent.com/78134499/110053368-9f8b2a80-7d9c-11eb-857a-dfce417111b2.png)






