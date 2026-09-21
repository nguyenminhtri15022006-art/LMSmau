import React from 'react';
import '../styles/ResultsHero.css';

function getResultMessage(score, userName) {
    if (score >= 80) {
        return {
            title: `Xuất sắc, ${userName}!`,
            subtitle: `Bạn đã hoàn thành xuất sắc bài kiểm tra — nắm vững kiến thức!`,
        };
    }
    if (score >= 40) {
        return {
            title: `Tốt, ${userName}!`,
            subtitle: 'Bạn đã hoàn thành bài kiểm tra. Hãy ôn lại các câu sai nhé.',
        };
    }
    return {
        title: `Cố gắng nhé, ${userName}!`,
        subtitle: 'Đừng nản chí! Hãy xem lại bài giảng và thử làm lại nhé.',
    };
}

const ResultsHero = ({
                         scorePercentage,
                         quizTitle,
                         correctCount,
                         totalQuestions,
                         timeTaken,
                         userName,
                     }) => {
    const message = getResultMessage(scorePercentage, userName);

    return (
        <div className="results-hero">
            <div className="score-gauge">
                <span className="score-value">{scorePercentage}%</span>
                <span className="score-label">Tổng điểm</span>
            </div>
            <div className="congrats-text">
                <h1>{message.title}</h1>
                <p>{message.subtitle}</p>
                <div className="stats-row">
                    <div className="stat-item">
            <span className="material-symbols-outlined" style={{color: '#64748b'}}>
              schedule
            </span>
                        Thời gian: {timeTaken}
                    </div>
                    <div className="stat-item">
            <span className="material-symbols-outlined" style={{color: '#10b981'}}>
              check_circle
            </span>
                        Đúng: {correctCount}/{totalQuestions}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ResultsHero;
