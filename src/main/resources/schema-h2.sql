DROP TABLE IF EXISTS book;

CREATE TABLE book (
    book_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(100) NOT NULL,
    author     VARCHAR(100) NOT NULL,
    price      INT          NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL
);
