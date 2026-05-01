const OTP_LENGTH = 6;

const toDigitString = (input) =>
  [...input]
    .map((ch) => ch.charCodeAt(0).toString())
    .join("")
    .replace(/\D/g, "");

const foldPairwise = (digits) => {
  let next = "";
  let left = 0;
  let right = digits.length - 1;

  while (left <= right) {
    if (left === right) {
      next += digits[left];
    } else {
      next += (Number(digits[left]) + Number(digits[right])).toString();
    }
    left += 1;
    right -= 1;
  }

  return next;
};

export const generateOtpFromEmailAndTime = (email, generatedAt) => {
  const seed = `${String(email || "").trim().toLowerCase()}${String(generatedAt || "")}`;
  let digits = toDigitString(seed);

  if (!digits) return "000000";

  while (digits.length > OTP_LENGTH) {
    digits = foldPairwise(digits);
  }

  return digits.padStart(OTP_LENGTH, "0").slice(-OTP_LENGTH);
};
