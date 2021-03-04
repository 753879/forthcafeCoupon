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