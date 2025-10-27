package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Favorite;
import org.example.server.entity.Product;
import org.example.server.entity.User;
import org.example.server.repository.FavoriteRepository;
import org.example.server.repository.ProductRepository;
import org.example.server.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;

    private User getUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public boolean toggle(Authentication auth, Long productId) {
        var user = getUser(auth);
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        var exist = favRepo.findByUserAndProduct(user, product);
        if (exist.isPresent()) {
            favRepo.delete(exist.get());
            return false;
        } else {
            Favorite f = new Favorite();
            f.setUser(user);
            f.setProduct(product);
            favRepo.save(f);
            return true;
        }
    }

    @Transactional
    public void add(Authentication auth, Long productId) {
        var user = getUser(auth);
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!favRepo.existsByUserAndProduct(user, product)) {
            Favorite f = new Favorite();
            f.setUser(user);
            f.setProduct(product);
            favRepo.save(f);
        }
    }

    @Transactional
    public void remove(Authentication auth, Long productId) {
        var user = getUser(auth);
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        favRepo.findByUserAndProduct(user, product).ifPresent(favRepo::delete);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stat(Authentication auth, Long productId) {
        var user = getUser(auth);
        var product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        boolean favorite = favRepo.existsByUserAndProduct(user, product);
        long count = favRepo.countByProduct(product);
        return Map.of("favorite", favorite, "count", count);
    }

    @Transactional(readOnly = true)
    public Page<Product> myFavorites(Authentication auth, int page, int size) {
        var user = getUser(auth);
        // trả về Page<Favorite> rồi map sang Page<Product> (nhanh gọn):
        var pf = favRepo.findByUserWithProductAndCategory(user, PageRequest.of(page, size));
        return pf.map(Favorite::getProduct);
    }
}
