package forthcafe;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CouponRepository extends PagingAndSortingRepository<Coupon, Long>{

    	List<Coupon> findByMenuId(Long menuId);

}