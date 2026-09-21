import "./SkeletonCourseCard.css";

export default function SkeletonCourseCard() {
    return (
        <div className="skeleton-card">
            <div className="skeleton-card__img" />

            <div className="skeleton-card__content">
                <div className="skeleton skeleton-card__title-line" />
                <div className="skeleton skeleton-card__title-line skeleton-card__title-line--short" />

                <div className="skeleton-card__instructor-row">
                    <div className="skeleton skeleton-card__icon" />
                    <div className="skeleton skeleton-card__instructor-text" />
                </div>

                <div className="skeleton-card__meta">
                    <div className="skeleton skeleton-card__pill" />
                    <div className="skeleton skeleton-card__pill" />
                    <div className="skeleton skeleton-card__pill" />
                </div>

                <div className="skeleton skeleton-card__btn" />
            </div>
        </div>
    );
}
