CREATE TABLE admin_user (
  id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  last_login_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_admin_user_username UNIQUE (username)
);

CREATE TABLE role (
  id BIGINT NOT NULL,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_role_code UNIQUE (code)
);

CREATE TABLE permission (
  id BIGINT NOT NULL,
  code VARCHAR(128) NOT NULL,
  name VARCHAR(128) NOT NULL,
  module VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_permission_code UNIQUE (code)
);

CREATE TABLE admin_user_role (
  id BIGINT NOT NULL,
  admin_user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_admin_user_role_user_role UNIQUE (admin_user_id, role_id)
);

CREATE TABLE role_permission (
  id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_role_permission_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE activity (
  id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  cover_file_id BIGINT,
  description TEXT,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  location VARCHAR(255) NOT NULL,
  capacity INT,
  registration_deadline DATETIME,
  owner_name VARCHAR(64),
  contact_phone VARCHAR(32),
  plan_content TEXT,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_activity_status ON activity (status);
CREATE INDEX idx_activity_start_time ON activity (start_time);

CREATE TABLE activity_process_item (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  time_label VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_activity_process_item_activity_id ON activity_process_item (activity_id);

CREATE TABLE activity_custom_field (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  field_key VARCHAR(64) NOT NULL,
  label VARCHAR(128) NOT NULL,
  field_type VARCHAR(32) NOT NULL,
  required TINYINT NOT NULL DEFAULT 0,
  options_json JSON,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_activity_custom_field_activity_key UNIQUE (activity_id, field_key)
);

CREATE TABLE registration (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  attendee_count INT NOT NULL DEFAULT 1,
  unit_name VARCHAR(128),
  age_group VARCHAR(64),
  remark VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  cancelled_by BIGINT,
  cancelled_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_registration_activity_phone UNIQUE (activity_id, phone)
);

CREATE INDEX idx_registration_activity_id ON registration (activity_id);
CREATE INDEX idx_registration_phone ON registration (phone);
CREATE INDEX idx_registration_status ON registration (status);

CREATE TABLE registration_custom_value (
  id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  custom_field_id BIGINT NOT NULL,
  field_key VARCHAR(64) NOT NULL,
  label VARCHAR(128) NOT NULL,
  value_text TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_registration_custom_value_registration_id ON registration_custom_value (registration_id);

CREATE TABLE check_in (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  check_in_time DATETIME NOT NULL,
  method VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  manual TINYINT NOT NULL DEFAULT 0,
  handled_by BIGINT,
  revoked_by BIGINT,
  revoked_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_check_in_registration_id UNIQUE (registration_id)
);

CREATE INDEX idx_check_in_activity_id ON check_in (activity_id);

CREATE TABLE volunteer_position (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  name VARCHAR(128) NOT NULL,
  description TEXT,
  capacity INT NOT NULL,
  service_start_time DATETIME NOT NULL,
  service_end_time DATETIME NOT NULL,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_volunteer_position_activity_id ON volunteer_position (activity_id);

CREATE TABLE volunteer_application (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  position_id BIGINT NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  unit_name VARCHAR(128),
  age_group VARCHAR(64),
  available_time_note VARCHAR(500),
  experience_note VARCHAR(500),
  remark VARCHAR(500),
  status VARCHAR(32) NOT NULL,
  reviewed_by BIGINT,
  reviewed_at DATETIME,
  review_note VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_volunteer_application_activity_phone UNIQUE (activity_id, phone)
);

CREATE INDEX idx_volunteer_application_position_id ON volunteer_application (position_id);
CREATE INDEX idx_volunteer_application_status ON volunteer_application (status);

CREATE TABLE volunteer_attendance (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  application_id BIGINT NOT NULL,
  check_in_time DATETIME,
  check_out_time DATETIME,
  service_minutes INT,
  status VARCHAR(32) NOT NULL,
  manually_adjusted TINYINT NOT NULL DEFAULT 0,
  adjusted_service_minutes INT,
  adjustment_reason VARCHAR(500),
  handled_by BIGINT,
  revoked_by BIGINT,
  revoked_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_volunteer_attendance_application_id UNIQUE (application_id)
);

CREATE INDEX idx_volunteer_attendance_activity_id ON volunteer_attendance (activity_id);

CREATE TABLE visitor_report (
  id BIGINT NOT NULL,
  activity_id BIGINT,
  visitor_unit VARCHAR(128) NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  contact_phone VARCHAR(32) NOT NULL,
  visitor_count INT NOT NULL,
  visit_date DATETIME NOT NULL,
  visit_reason VARCHAR(500),
  remark VARCHAR(500),
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_visitor_report_activity_id ON visitor_report (activity_id);

CREATE TABLE survey (
  id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT,
  updated_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_survey_activity_id UNIQUE (activity_id)
);

CREATE TABLE survey_question (
  id BIGINT NOT NULL,
  survey_id BIGINT NOT NULL,
  title VARCHAR(500) NOT NULL,
  type VARCHAR(32) NOT NULL,
  required TINYINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_survey_question_survey_id ON survey_question (survey_id);

CREATE TABLE survey_option (
  id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  label VARCHAR(255) NOT NULL,
  `value` VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_survey_option_question_id ON survey_option (question_id);

CREATE TABLE survey_response (
  id BIGINT NOT NULL,
  survey_id BIGINT NOT NULL,
  registration_id BIGINT NOT NULL,
  respondent_name VARCHAR(64) NOT NULL,
  respondent_phone VARCHAR(32) NOT NULL,
  submitted_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_survey_response_survey_registration UNIQUE (survey_id, registration_id)
);

CREATE TABLE survey_answer (
  id BIGINT NOT NULL,
  response_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  option_id BIGINT,
  option_ids_json JSON,
  numeric_value DECIMAL(5,2),
  text_value TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT ux_survey_answer_response_question UNIQUE (response_id, question_id)
);

CREATE INDEX idx_survey_answer_response_id ON survey_answer (response_id);

CREATE TABLE file_asset (
  id BIGINT NOT NULL,
  activity_id BIGINT,
  category VARCHAR(32) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(128) NOT NULL,
  extension VARCHAR(32) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  uploaded_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX idx_file_asset_activity_category ON file_asset (activity_id, category);

CREATE TABLE operation_log (
  id BIGINT NOT NULL,
  admin_user_id BIGINT,
  admin_username VARCHAR(64) NOT NULL,
  admin_role_code VARCHAR(64) NOT NULL,
  action VARCHAR(128) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id BIGINT,
  target_summary VARCHAR(255),
  ip VARCHAR(64),
  user_agent VARCHAR(500),
  detail_json JSON,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id)
);

CREATE INDEX idx_operation_log_admin_user_id ON operation_log (admin_user_id);
CREATE INDEX idx_operation_log_action ON operation_log (action);
CREATE INDEX idx_operation_log_target ON operation_log (target_type, target_id);
CREATE INDEX idx_operation_log_created_at ON operation_log (created_at);
