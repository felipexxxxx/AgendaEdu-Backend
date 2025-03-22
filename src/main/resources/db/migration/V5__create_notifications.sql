CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    atividade_id BIGINT NOT NULL,
    mensagem VARCHAR(255) NOT NULL,
    data_envio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (atividade_id) REFERENCES activities(id) ON DELETE CASCADE
);
