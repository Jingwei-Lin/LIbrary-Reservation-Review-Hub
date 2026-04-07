import { useEffect, useState } from 'react';
import ReactDOM from 'react-dom';

const Portal = ({ children }) => {
    const [portalElement, setPortalElement] = useState(null);

    useEffect(() => {
        // Create a div for the portal
        const element = document.createElement('div');
        element.id = 'react-portal';
        document.body.appendChild(element);
        setPortalElement(element);

        // Cleanup
        return () => {
            document.body.removeChild(element);
        };
    }, []);

    if (!portalElement) return null;

    return ReactDOM.createPortal(children, portalElement);
};

export default Portal;