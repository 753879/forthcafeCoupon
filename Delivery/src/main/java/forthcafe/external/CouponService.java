package forthcafe.external;

import org.springframework.cloud.openfeign.FeignClient; 
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

// feignclient는 인터페이스 기술로 사용 가능
// url: 호출하고싶은 서비스 주소. http://localhost:8085 - application.yaml에 정의

@FeignClient(name="Coupon", url="${api.url.coupon}") 
public interface CouponService {

    @RequestMapping(method = RequestMethod.POST, path = "/coupons", consumes = "application/json")
    public void coupon(@RequestBody Coupon coupon);

}