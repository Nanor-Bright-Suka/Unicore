
CREATE TABLE course (
                        course_id UUID PRIMARY KEY,
                        code VARCHAR(255) NOT NULL UNIQUE,
                        title VARCHAR(255) NOT NULL,
                        description VARCHAR(2000),
                        state VARCHAR(50) NOT NULL,
                        semester VARCHAR(50) NOT NULL,
                        academic_year VARCHAR(20) NOT NULL,
                        max_capacity INT NOT NULL,
                        lecturer_id UUID NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP NOT NULL,

                        CONSTRAINT fk_course_lecturer
                            FOREIGN KEY (lecturer_id)
                                REFERENCES my_users(user_id)
                                ON DELETE CASCADE
);