CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    scenario VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    owner_team VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'Draft',
    current_version_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_prompt_template_scenario (scenario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    version_label VARCHAR(32) NOT NULL,
    template_text MEDIUMTEXT NOT NULL,
    -- Standard JSON array of Prompt variables: [{"name":"...","description":"...","required":true}]
    variables_json JSON NOT NULL,
    output_format VARCHAR(32) NOT NULL DEFAULT 'JSON',
    status VARCHAR(32) NOT NULL DEFAULT 'Draft',
    author VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prompt_version_label (template_id, version_label),
    KEY idx_prompt_version_template (template_id),
    CONSTRAINT fk_prompt_version_template
        FOREIGN KEY (template_id) REFERENCES prompt_template(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS eval_dataset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dataset_key VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    scenario VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_eval_dataset_scenario (scenario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS eval_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dataset_id BIGINT NOT NULL,
    case_key VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    input_text TEXT NOT NULL,
    expected_text TEXT NOT NULL,
    risk_level VARCHAR(32) NOT NULL DEFAULT 'normal',
    tags_json JSON,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_eval_case_key (dataset_id, case_key),
    KEY idx_eval_case_dataset (dataset_id),
    CONSTRAINT fk_eval_case_dataset
        FOREIGN KEY (dataset_id) REFERENCES eval_dataset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS score_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    rule_type VARCHAR(40) NOT NULL,
    rule_value VARCHAR(512),
    expected_format VARCHAR(32),
    weight DECIMAL(6,2) NOT NULL DEFAULT 1.00,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_score_rule_case (case_id),
    CONSTRAINT fk_score_rule_case
        FOREIGN KEY (case_id) REFERENCES eval_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS eval_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_key VARCHAR(80) NOT NULL UNIQUE,
    prompt_version_id BIGINT NOT NULL,
    dataset_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'queued',
    total_cases INT NOT NULL DEFAULT 0,
    passed_cases INT NOT NULL DEFAULT 0,
    failed_cases INT NOT NULL DEFAULT 0,
    risk_case_count INT NOT NULL DEFAULT 0,
    pass_rate DECIMAL(6,2) NOT NULL DEFAULT 0,
    average_latency_ms INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_eval_run_prompt_version (prompt_version_id),
    KEY idx_eval_run_dataset (dataset_id),
    CONSTRAINT fk_eval_run_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_version(id),
    CONSTRAINT fk_eval_run_dataset
        FOREIGN KEY (dataset_id) REFERENCES eval_dataset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS eval_case_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    eval_run_id BIGINT NOT NULL,
    eval_case_id BIGINT NOT NULL,
    result_status VARCHAR(32) NOT NULL,
    score DECIMAL(6,2) NOT NULL,
    latency_ms INT NOT NULL,
    failure_reasons_json JSON NOT NULL,
    output_summary VARCHAR(512),
    mock_output MEDIUMTEXT,
    explanation VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_eval_case_result_run (eval_run_id),
    KEY idx_eval_case_result_case (eval_case_id),
    CONSTRAINT fk_eval_case_result_run
        FOREIGN KEY (eval_run_id) REFERENCES eval_run(id),
    CONSTRAINT fk_eval_case_result_case
        FOREIGN KEY (eval_case_id) REFERENCES eval_case(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS generation_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    eval_run_id BIGINT,
    eval_case_id BIGINT,
    prompt_version_id BIGINT NOT NULL,
    input_summary VARCHAR(512),
    output_summary VARCHAR(512),
    latency_ms INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_reason VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_generation_trace_run (eval_run_id),
    KEY idx_generation_trace_prompt_version (prompt_version_id),
    CONSTRAINT fk_generation_trace_run
        FOREIGN KEY (eval_run_id) REFERENCES eval_run(id),
    CONSTRAINT fk_generation_trace_case
        FOREIGN KEY (eval_case_id) REFERENCES eval_case(id),
    CONSTRAINT fk_generation_trace_prompt_version
        FOREIGN KEY (prompt_version_id) REFERENCES prompt_version(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
