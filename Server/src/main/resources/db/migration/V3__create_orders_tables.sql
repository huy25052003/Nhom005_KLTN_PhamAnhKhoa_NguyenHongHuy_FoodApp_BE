
CREATE TABLE IF NOT EXISTS orders (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      total DECIMAL(19,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_orders_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    );


CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    quantity INT NOT NULL,
    price DECIMAL(19,2) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    CONSTRAINT fk_items_order
    FOREIGN KEY (order_id) REFERENCES orders(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_items_product
    FOREIGN KEY (product_id) REFERENCES products(id)
    );
