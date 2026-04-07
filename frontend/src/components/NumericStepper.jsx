import React from 'react';

const clamp = (val, min, max) => {
  let v = Number(val);
  if (!Number.isFinite(v)) v = 0;
  if (typeof min === 'number') v = Math.max(min, v);
  if (typeof max === 'number') v = Math.min(max, v);
  return v;
};

const NumericStepper = ({ value, onChange, min, max, step = 1, disabled = false }) => {
  const safeValue = Number.isFinite(Number(value)) ? Number(value) : (typeof min === 'number' ? min : 0);

  const handleInputChange = (e) => {
    const num = clamp(e.target.value, min, max);
    onChange?.(num);
  };

  const handleInc = () => {
    const next = clamp(safeValue + step, min, max);
    onChange?.(next);
  };

  const handleDec = () => {
    const next = clamp(safeValue - step, min, max);
    onChange?.(next);
  };

  return (
    <div className="number-stepper">
      <input
        className="input"
        type="number"
        value={safeValue}
        onChange={handleInputChange}
        min={min}
        max={max}
        step={step}
        disabled={disabled}
      />
      <div className="stepper-vertical">
        <button type="button" className="step-btn step-up" onClick={handleInc} disabled={disabled}>▲</button>
        <button type="button" className="step-btn step-down" onClick={handleDec} disabled={disabled}>▼</button>
      </div>
    </div>
  );
};

export default NumericStepper;