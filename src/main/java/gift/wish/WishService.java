package gift.wish;

import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<WishResponse> getWishes(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable).map(WishResponse::from);
    }

    public record AddWishResult(Wish wish, boolean created) {
    }

    public AddWishResult addWish(Long memberId, WishRequest request) {
        var product = productRepository.findById(request.productId())
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + request.productId()));

        var existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId()).orElse(null);
        if (existing != null) {
            return new AddWishResult(existing, false);
        }

        var saved = wishRepository.save(new Wish(memberId, product));
        return new AddWishResult(saved, true);
    }

    public void removeWish(Long memberId, Long wishId) {
        var wish = wishRepository.findById(wishId)
            .orElseThrow(() -> new NoSuchElementException("위시가 존재하지 않습니다. id=" + wishId));

        if (!wish.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 위시만 삭제할 수 있습니다.");
        }

        wishRepository.delete(wish);
    }
}
