import React, { useEffect, useState } from "react";
import { identityApi } from "../../../../../services/identity.api";
import "../../../styles/student/DetailCourse/InstructorSection.css";

const ROLE_LABEL = {
    INSTRUCTOR: "Giảng viên",
    ADMIN: "Quản trị viên",
    STUDENT: "Học viên",
};

export default function InstructorSection({ instructorId }) {
    const [instructor, setInstructor] = useState(null);
    const [loading, setLoading] = useState(false);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        if (!instructorId) return;

        setLoading(true);
        setFailed(false);

        identityApi.getPublicProfile(instructorId)
            .then((data) => {
                setInstructor(data);
            })
            .catch((err) => {
                console.error("getPublicProfile failed:", err?.response?.status, err?.response?.data);
                setFailed(true);
            })
            .finally(() => setLoading(false));
    }, [instructorId]);

    if (!instructorId) return null;

    if (loading) {
        return (
            <section className="instructor-section">
                <h2 className="instructor-section__title">Giảng viên</h2>
                <div className="instructor-section__card">
                    <div className="instructor-section__avatar instructor-section__avatar--skeleton" />
                    <div className="instructor-section__info">
                        <div className="instructor-section__skeleton-line instructor-section__skeleton-line--name" />
                        <div className="instructor-section__skeleton-line" />
                        <div className="instructor-section__skeleton-line instructor-section__skeleton-line--long" />
                    </div>
                </div>
            </section>
        );
    }

    if (failed || !instructor) {
        return (
            <section className="instructor-section">
                <h2 className="instructor-section__title">Giảng viên</h2>
                <div className="instructor-section__card">
                    <div className="instructor-section__avatar-wrapper">
                        <div className="instructor-section__avatar">
                            <span className="material-symbols-outlined instructor-section__avatar-placeholder">person</span>
                        </div>
                    </div>
                    <div className="instructor-section__info">
                        <p className="instructor-section__job-title">Thông tin giảng viên chưa được cập nhật.</p>
                    </div>
                </div>
            </section>
        );
    }

    return (
        <section className="instructor-section">
            <h2 className="instructor-section__title">Giảng viên</h2>
            <div className="instructor-section__card">
                <div className="instructor-section__avatar-wrapper">
                    <div
                        className="instructor-section__avatar"
                        style={instructor.avatarUrl
                            ? { backgroundImage: `url(${instructor.avatarUrl})` }
                            : {}
                        }
                    >
                        {!instructor.avatarUrl && (
                            <span className="material-symbols-outlined instructor-section__avatar-placeholder">
                                person
                            </span>
                        )}
                    </div>
                </div>

                <div className="instructor-section__info">
                    <h3 className="instructor-section__name">
                        {instructor.fullname || "Giảng viên"}
                    </h3>
                    {instructor.role && (
                        <p className="instructor-section__job-title">
                            {ROLE_LABEL[instructor.role] ?? instructor.role}
                        </p>
                    )}
                    {instructor.bio && (
                        <p className="instructor-section__bio">{instructor.bio}</p>
                    )}
                </div>
            </div>
        </section>
    );
}
