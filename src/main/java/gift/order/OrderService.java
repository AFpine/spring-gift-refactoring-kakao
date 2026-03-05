package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishRepository wishRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Page<OrderResponse> getOrders(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable).map(OrderResponse::from);
    }

    // order flow:
    // 1. validate option
    // 2. subtract stock
    // 3. deduct points
    // 4. save order
    @Transactional
    public Order createOrder(Member member, OrderRequest request) {
        // validate option
        var option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return null;
        }

        // subtract stock
        option.subtractQuantity(request.quantity());
        optionRepository.save(option);

        // deduct points
        int price = option.calculatePrice(request.quantity());
        member.deductPoint(price);
        memberRepository.save(member);

        // save order
        Order saved = orderRepository.save(new Order(option, member.getId(), request.quantity(), request.message()));

        // cleanup wish
        wishRepository.deleteByMemberIdAndProductId(member.getId(), option.getProduct().getId());

        return saved;
    }

    public void sendKakaoMessageIfPossible(Member member, Order order) {
        if (member.getKakaoAccessToken() == null) {
            return;
        }
        try {
            var product = order.getOption().getProduct();
            kakaoMessageClient.sendToMe(member.getKakaoAccessToken(), order, product);
        } catch (Exception ignored) {
        }
    }
}
