import React from 'react';
import {Link} from 'react-router-dom';

const ExerciseBreadcrumb = ({items = []}) => {
    return (
        <nav className="ch-breadcrumb" style={{marginBottom: '32px'}}>
            <Link to="/exercises">Bài tập</Link>
            {items.map((item, index) => (
                <React.Fragment key={index}>
          <span className="ch-breadcrumb-separator"
                style={{margin: '0 8px', display: 'inline-flex', alignItems: 'center'}}>
            <span className="material-symbols-outlined" style={{fontSize: '16px'}}>chevron_right</span>
          </span>
                    {item.link ? (
                        <Link to={item.link} state={item.state} className="ch-breadcrumb-current">
                            {item.label}
                        </Link>
                    ) : (
                        <span className="ch-breadcrumb-current">{item.label}</span>
                    )}
                </React.Fragment>
            ))}
        </nav>
    );
};

export default ExerciseBreadcrumb;
