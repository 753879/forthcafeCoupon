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
            coupon.setPiece(deliveryCancelled.getQuantity());

            couponRepository.save(coupon);



        }
    }

}
