import React, { useRef, useEffect } from 'react';
import './OtpInput.css';

/**
 * 6-digit one-time-code input with autoFocus, paste, Backspace and arrow-key handling.
 * Controlled by a string `value` of length up to `length` (digits only).
 */
export default function OtpInput({
  value,
  onChange,
  length = 6,
  autoFocus = true,
  disabled = false,
  ariaLabelPrefix = 'Цифра',
}) {
  const inputsRef = useRef([]);

  const digits = Array.from({ length }, (_, i) => value[i] || '');

  useEffect(() => {
    if (autoFocus) {
      inputsRef.current[0]?.focus();
    }
  }, [autoFocus]);

  const update = (next) => {
    onChange(next.join('').slice(0, length));
  };

  const setDigit = (i, raw) => {
    const cleaned = raw.replace(/\D/g, '');
    if (cleaned.length <= 1) {
      const next = [...digits];
      next[i] = cleaned;
      update(next);
      if (cleaned && i < length - 1) inputsRef.current[i + 1]?.focus();
    } else {
      const chars = cleaned.slice(0, length - i).split('');
      const next = [...digits];
      chars.forEach((ch, k) => { next[i + k] = ch; });
      update(next);
      const last = Math.min(i + chars.length, length - 1);
      inputsRef.current[last]?.focus();
    }
  };

  const handleKeyDown = (i, e) => {
    if (e.key === 'Backspace' && !digits[i] && i > 0) {
      inputsRef.current[i - 1]?.focus();
    }
    if (e.key === 'ArrowLeft' && i > 0) inputsRef.current[i - 1]?.focus();
    if (e.key === 'ArrowRight' && i < length - 1) inputsRef.current[i + 1]?.focus();
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length);
    if (!pasted) return;
    const next = Array(length).fill('');
    pasted.split('').forEach((ch, i) => { next[i] = ch; });
    update(next);
    const last = Math.min(pasted.length, length) - 1;
    inputsRef.current[last]?.focus();
  };

  return (
    <div className="otp-row" onPaste={handlePaste}>
      {digits.map((d, i) => (
        <input
          key={i}
          ref={(el) => (inputsRef.current[i] = el)}
          className="otp-input"
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={length}
          value={d}
          onChange={(e) => setDigit(i, e.target.value)}
          onKeyDown={(e) => handleKeyDown(i, e)}
          autoComplete="one-time-code"
          disabled={disabled}
          aria-label={`${ariaLabelPrefix} ${i + 1}`}
        />
      ))}
    </div>
  );
}
