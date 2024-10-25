CREATE TABLE IF NOT EXISTS messages
(
    id                     INT PRIMARY KEY AUTO_INCREMENT,
    content                VARCHAR(255) NOT NULL,  -- Added length for VARCHAR
    content_type           VARCHAR(128) NOT NULL,
    room_id                VARCHAR(255) NOT NULL,
    sent                   TIMESTAMP    NOT NULL,
    username               VARCHAR(60)  NOT NULL,
    user_id               VARCHAR(60)  NOT NULL,
    user_avatar_image_link VARCHAR(256) NOT NULL,
    reply_to_message_id INT
);
