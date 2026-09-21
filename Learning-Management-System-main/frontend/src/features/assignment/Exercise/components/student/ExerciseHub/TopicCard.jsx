import React from 'react';
import {Link} from 'react-router-dom';
import {motion} from 'framer-motion';
import '../../../styles/student/ExerciseHub/TopicCard.css';

const MotionLink = motion(Link);

const TopicCard = ({topic, index = 0}) => {
    const getBadgeStyle = (topicId) => {
        const styles = {
            default: {background: '#dcfce7', color: '#166534'}
        };
        return styles[topicId] || styles.default;
    };

    return (
        <MotionLink
            to={`/exercises/code/${topic.slug}`}
            className="topic-card"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.38, ease: 'easeOut', delay: Math.min(index * 0.06, 0.36) }}
            whileHover={{ y: -6 }}
        >
            <div className="topic-image">
                <img src={topic.image} alt={topic.title}/>
            </div>
            <div className="topic-content">
        <span
            className={`topic-badge ${topic.id === 'java' ? 'status-draft' : 'status-published'}`}
            style={getBadgeStyle(topic.id)}
        >
          {topic.level}
        </span>
                <h3 className="topic-title">{topic.title}</h3>
                <p className="topic-desc">{topic.description}</p>
                <div style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '16px'
                }}>
          <span style={{fontSize: '12px', color: '#94a3b8'}}>

            {topic.time}
          </span>
                </div>
                <span className="solve-btn topic-card__action">
                    Luyện tập ngay
                </span>
            </div>
        </MotionLink>
    );
};

export default TopicCard;
