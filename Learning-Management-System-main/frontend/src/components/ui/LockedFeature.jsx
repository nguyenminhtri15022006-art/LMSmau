import React from 'react';
import { Link } from 'react-router-dom';
import useAuth from '../../hooks/useAuth';
import './styles/LockedFeature.css';

/**
 * LockedFeature - A wrapper that shows a "Please Login" message
 * if the user is not authenticated.
 * 
 * @param {Object} props
 * @param {React.ReactNode} props.children - Content to show if authenticated
 * @param {string} props.featureName - Name of the feature being locked
 */
const LockedFeature = ({ children, featureName = 'this feature' }) => {
    const { isAuthenticated } = useAuth();

    if (isAuthenticated) {
        return <>{children}</>;
    }

    return (
        <div className="locked-feature-container">
            <div className="locked-feature-content">
                <div className="locked-icon">
                    <span className="material-symbols-outlined">lock</span>
                </div>
                <h2>Feature Locked</h2>
                <p>Please log in to access <strong>{featureName}</strong> and your personalized learning data.</p>
                <div className="locked-actions">
                    <Link to="/login" className="btn-primary">Sign In</Link>
                    <Link to="/register" className="btn-secondary">Create Account</Link>
                </div>
            </div>
        </div>
    );
};

export default LockedFeature;
