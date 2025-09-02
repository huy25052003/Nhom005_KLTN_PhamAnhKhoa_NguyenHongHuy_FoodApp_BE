package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.*;
import org.example.server.repository.CartRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    private User resolveUser(Authentication auth) {
        String login = auth.getName();
        // Tuỳ bạn dùng email hay username làm "name"
        return userRepo.findByUsername(login)
                .orElseGet(() -> userRepo.findByUsername(login)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user: " + login)));
    }

    private Cart getOrCreate(User user) {
        return cartRepo.findByUser(user).orElseGet(() -> cartRepo.save(Cart.builder().user(user).build()));
    }

    private Cart init(Cart c) {
        c.getItems().forEach(it -> { if (it.getProduct() != null) it.getProduct().getId(); });
        return c;
    }

    public Cart getCart(Authentication auth) {
        return init(getOrCreate(resolveUser(auth)));
    }

    @Transactional
    public CartItem addItem(Authentication auth, Long productId, int quantity) {
        var user = resolveUser(auth);
        var cart = getOrCreate(user);
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        var exist = cart.getItems().stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);

        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            return exist;
        }
        var item = CartItem.builder().cart(cart).product(product).quantity(quantity).build();
        cart.getItems().add(item);
        cartRepo.save(cart);
        return item;
    }

    @Transactional
    public CartItem updateItem(Authentication auth, Long itemId, int quantity) {
        var user = resolveUser(auth);
        var cart = getOrCreate(user);
        var it = cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dòng giỏ hàng"));
        it.setQuantity(quantity);
        cartRepo.save(cart);
        return it;
    }

    @Transactional
    public void removeItem(Authentication auth, Long itemId) {
        var user = resolveUser(auth);
        var cart = getOrCreate(user);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        cartRepo.save(cart);
    }

    @Transactional
    public void clear(Authentication auth) {
        var user = resolveUser(auth);
        var cart = getOrCreate(user);
        cart.getItems().clear();
        cartRepo.save(cart);
    }
}
