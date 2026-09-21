import { describe, it, expect, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import React from 'react';

// Mock FontAwesome to avoid SVG rendering issues in jsdom
vi.mock('@fortawesome/react-fontawesome', () => ({
  FontAwesomeIcon: ({ icon }) => React.createElement('span', { 'data-icon': icon?.iconName || 'mock' }, ''),
}));

vi.mock('@fortawesome/free-solid-svg-icons', () => ({
  faCheckCircle: { iconName: 'check-circle' },
  faTimesCircle: { iconName: 'times-circle' },
  faInfoCircle: { iconName: 'info-circle' },
  faExclamationTriangle: { iconName: 'exclamation-triangle' },
  faTimes: { iconName: 'times' },
}));

import { ToastProvider, useToast } from '../../components/ui/Toast';

// Test component that triggers each toast type
function ToastTrigger() {
  const toast = useToast();
  return (
    <div>
      <button data-testid="btn-success" onClick={() => toast.success('Success msg', 'Success title')}>
        Show Success
      </button>
      <button data-testid="btn-error" onClick={() => toast.error('Error msg')}>
        Show Error
      </button>
      <button data-testid="btn-info" onClick={() => toast.info('Info msg', 'Info title')}>
        Show Info
      </button>
      <button data-testid="btn-confirm" onClick={() => toast.confirm('Confirm?', vi.fn(), { title: 'Confirm title', confirmLabel: 'Yes', cancelLabel: 'No' })}>
        Show Confirm
      </button>
    </div>
  );
}

function ConfirmTrigger({ onConfirm }) {
  const toast = useToast();
  return (
    <button data-testid="btn-confirm-controlled" onClick={() => toast.confirm('Confirm controlled?', onConfirm, { confirmLabel: 'Yes', cancelLabel: 'No' })}>
      Show Confirm
    </button>
  );
}

const renderToast = () => render(
  <ToastProvider>
    <ToastTrigger />
  </ToastProvider>
);

describe('ToastProvider and useToast', () => {
  it('throws error when useToast is used outside provider', () => {
    const TestComp = () => { useToast(); return null; };
    expect(() => renderToString(React.createElement(TestComp))).toThrow('useToast must be used inside <ToastProvider>');
  });

  it('renders success toast on trigger', async () => {
    renderToast();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('btn-success'));
    expect(screen.getByText('Success msg')).toBeInTheDocument();
    expect(screen.getByText('Success title')).toBeInTheDocument();
  });

  it('renders error toast with default title', async () => {
    renderToast();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('btn-error'));
    expect(screen.getByText('Error msg')).toBeInTheDocument();
    expect(screen.getByText('Lỗi')).toBeInTheDocument();
  });

  it('renders info toast', async () => {
    renderToast();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('btn-info'));
    expect(screen.getByText('Info msg')).toBeInTheDocument();
    expect(screen.getByText('Info title')).toBeInTheDocument();
  });

  it('renders confirm toast with actions', async () => {
    renderToast();
    const user = userEvent.setup();
    await user.click(screen.getByTestId('btn-confirm'));
    expect(screen.getByText('Confirm?')).toBeInTheDocument();
    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
  });

  it('removes toast when close button is clicked', async () => {
    const { container } = renderToast();
    const user = userEvent.setup();

    await user.click(screen.getByTestId('btn-success'));
    await user.click(container.querySelector('.toast-close'));

    await waitFor(() => {
      expect(screen.queryByText('Success msg')).not.toBeInTheDocument();
    });
  });

  it('auto removes regular toasts after the duration', async () => {
    vi.useFakeTimers();
    try {
      renderToast();

      fireEvent.click(screen.getByTestId('btn-info'));
      expect(screen.getByText('Info msg')).toBeInTheDocument();

      act(() => {
        vi.advanceTimersByTime(4260);
      });

      expect(screen.queryByText('Info msg')).not.toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });

  it('runs confirm callback when confirm action is clicked', async () => {
    const onConfirm = vi.fn();
    render(
      <ToastProvider>
        <ConfirmTrigger onConfirm={onConfirm} />
      </ToastProvider>
    );

    fireEvent.click(screen.getByTestId('btn-confirm-controlled'));
    fireEvent.click(screen.getByText('Yes'));

    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('dismisses confirm toast without callback when cancel action is clicked', async () => {
    const onConfirm = vi.fn();
    render(
      <ToastProvider>
        <ConfirmTrigger onConfirm={onConfirm} />
      </ToastProvider>
    );

    fireEvent.click(screen.getByTestId('btn-confirm-controlled'));
    fireEvent.click(screen.getByText('No'));

    expect(onConfirm).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(screen.queryByText('Confirm controlled?')).not.toBeInTheDocument();
    });
  });
});
