import React from 'react';
import {useNavigate} from 'react-router-dom';
import '../../../styles/student/ExerciseListing/ChallengeCard.css';

const ChallengeCard = ({challenge, courseTitle, courseSlug}) => {
    const navigate = useNavigate();
    const rateValue = parseInt(challenge.successRate);
    const rateColor = rateValue >= 70 ? '#10b981' : rateValue >= 40 ? '#f59e0b' : '#ef4444';

    const handleClick = () => {
        const challengePath = challenge.slug
            ? `/exercises/challenge/${challenge.slug}`
            : `/exercises/challenge/${challenge.id}`;

        navigate(challengePath, {
            state: {courseTitle, courseSlug}
        });
    };

    return (
        <div className={`challenge-item-card ${challenge.completed ? 'is-completed' : ''}`}>
            <div className="challenge-main-info">
                <div className="challenge-icon-box">
          <span className="material-symbols-outlined">
            {challenge.difficulty === 'EASY' ? 'eco' : challenge.difficulty === 'MEDIUM' ? 'bolt' : 'psychology'}
          </span>
                </div>
                <div className="challenge-text">
                    <h3>{challenge.title}</h3>
                    <div className="challenge-meta-row">
            <span className={`meta-badge diff-${challenge.difficulty.toLowerCase()}`}>
              {challenge.difficulty === 'EASY' ? 'Dễ' : challenge.difficulty === 'MEDIUM' ? 'Trung bình' : 'Khó'}
            </span>
                        <span className="meta-text" style={{color: rateColor}}>
              <span className="material-symbols-outlined" style={{fontSize: '14px'}}>check_circle</span>
                            {challenge.successRate} thành công
            </span>
                    </div>
                </div>
            </div>
            <div className="challenge-actions">
                {challenge.status === 'Completed' ? (
                    <button onClick={handleClick} className="review-solution-btn">
                        Xem lời giải
                    </button>
                ) : (
                    <button onClick={handleClick} className="solve-btn">
                        Bắt đầu thử thách
                    </button>
                )}
            </div>
            {challenge.completed && (
                <span className="completion-check-icon material-symbols-outlined" title="Đã hoàn thành" aria-label="Đã hoàn thành">
                    check_circle
                </span>
            )}
        </div>
    );
};

export default ChallengeCard;
