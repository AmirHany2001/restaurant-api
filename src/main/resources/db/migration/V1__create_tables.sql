-- The schema as Hibernate had created it (ddl-auto=update) before Flyway was introduced.
-- Never edit this file once it has run anywhere: put every later change in a new V2__..., V3__... file.

CREATE TABLE categories (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
);

CREATE TABLE users (
    id           BIGINT                NOT NULL AUTO_INCREMENT,
    address      VARCHAR(255)          NOT NULL,
    email        VARCHAR(255)          NOT NULL,
    full_name    VARCHAR(255)          NOT NULL,
    password     VARCHAR(255)          NOT NULL,
    phone_number VARCHAR(255)          NOT NULL,
    role         ENUM ('ADMIN','USER') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE items (
    id          BIGINT                            NOT NULL AUTO_INCREMENT,
    description VARCHAR(255)                      NOT NULL,
    name        VARCHAR(255)                      NOT NULL,
    status      ENUM ('available','not_available') NOT NULL,
    category_id BIGINT                            DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_items_name UNIQUE (name),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE item_prices (
    item_id BIGINT         NOT NULL,
    price   DECIMAL(10, 2) NOT NULL,
    size    TINYINT        NOT NULL,
    PRIMARY KEY (item_id, size),
    CONSTRAINT fk_item_prices_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT chk_item_prices_size CHECK (size BETWEEN 0 AND 2)
);

CREATE TABLE orders (
    id          BIGINT                                              NOT NULL AUTO_INCREMENT,
    created_at  DATETIME(6)                                         NOT NULL,
    status      ENUM ('approved','cancelled','completed','pending') NOT NULL,
    total_price DECIMAL(12, 2)                                      NOT NULL,
    user_id     BIGINT                                              DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE order_items (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    notes               VARCHAR(255)   DEFAULT NULL,
    quantity            INT            NOT NULL,
    size                VARCHAR(255)   NOT NULL,
    unit_price_at_order DECIMAL(10, 2) NOT NULL,
    item_id             BIGINT         NOT NULL,
    order_id            BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
