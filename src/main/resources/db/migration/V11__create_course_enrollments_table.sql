
CREATE TABLE course_enrollments (
                                    enrollment_id UUID PRIMARY KEY,

                                    course_id UUID NOT NULL,
                                    student_id UUID NOT NULL,

                                    enrolled_at TIMESTAMP NOT NULL,

                                    is_deleted BOOLEAN NOT NULL,

                                    CONSTRAINT fk_enrollment_course
                                        FOREIGN KEY (course_id)
                                            REFERENCES course(course_id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT fk_enrollment_student
                                        FOREIGN KEY (student_id)
                                            REFERENCES my_users(user_id)
                                            ON DELETE CASCADE,

                                    CONSTRAINT uq_course_student
                                        UNIQUE (course_id, student_id)
);

