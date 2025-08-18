-- Clear existing data (optional, remove if you want to keep existing data)
TRUNCATE TABLE banks CASCADE;
TRUNCATE TABLE categories CASCADE;

-- Insert all 21 supported banks
INSERT INTO banks (code, name, support_level, is_active) VALUES
-- Fully supported banks
('hdfc', 'HDFC Bank', 'full', true),
('sbi', 'State Bank of India', 'full', true),
('icici', 'ICICI Bank', 'full', true),
('axis', 'Axis Bank', 'full', true),
('federal', 'Federal Bank', 'full', true),
('indian', 'Indian Bank', 'full', true),
('kotak', 'Kotak Mahindra Bank', 'full', true),

-- Well supported banks
('bob', 'Bank of Baroda', 'experimental', true),
('canara', 'Canara Bank', 'experimental', true),
('pnb', 'Punjab National Bank', 'experimental', true),
('idbi', 'IDBI Bank', 'experimental', true),
('karnataka', 'Karnataka Bank', 'experimental', true),
('idfc', 'IDFC First Bank', 'experimental', true),
('union', 'Union Bank of India', 'experimental', true),
('hsbc', 'HSBC Bank', 'experimental', true),
('centralbank', 'Central Bank of India', 'experimental', true),

-- Payment services and fintech
('jupiter', 'Jupiter', 'experimental', true),
('jiopayments', 'Jio Payments Bank', 'experimental', true),
('juspay', 'Juspay', 'experimental', true),
('slice', 'Slice', 'experimental', true),
('utkarsh', 'Utkarsh Small Finance Bank', 'experimental', true);

-- Insert categories (matching Kotlin CategoryMapping exactly)
INSERT INTO categories (code, name, icon, color, keywords) VALUES
('Food & Dining', 'Food & Dining', '🍔', '#FC8019', '["swiggy", "zomato", "dominos", "pizza", "burger", "kfc", "mcdonalds", "restaurant", "cafe", "food", "starbucks", "haldiram", "barbeque", "subway", "pizzahut", "burgerking"]'),
('Groceries', 'Groceries', '🛒', '#5AC85A', '["bigbasket", "blinkit", "zepto", "grofers", "jiomart", "dmart", "reliance fresh", "more", "grocery", "dunzo", "star bazaar", "spencer"]'),
('Transportation', 'Transportation', '🚗', '#000000', '["uber", "ola", "rapido", "metro", "irctc", "redbus", "makemytrip", "goibibo", "petrol", "fuel", "parking", "toll", "fastag", "indigo", "air india", "spicejet", "vistara", "cleartrip", "yatra", "abhibus", "bharat petroleum", "indian oil", "hp petrol"]'),
('Shopping', 'Shopping', '🛍️', '#FF9900', '["amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal", "shopclues", "firstcry", "pepperfry", "urban ladder", "store", "tatacliq", "lenskart", "tanishq", "croma", "reliance digital", "vijay sales"]'),
('Bills & Utilities', 'Bills & Utilities', '📄', '#4CAF50', '["electricity", "water", "gas", "broadband", "wifi", "internet", "tata sky", "dish", "d2h", "bill", "tata power", "adani", "bses", "act fibernet", "airtel fiber", "jio fiber", "hathway", "excitel", "mahanagar gas"]'),
('Entertainment', 'Entertainment', '🎬', '#E50914', '["netflix", "spotify", "prime", "hotstar", "sony liv", "zee5", "voot", "youtube", "cinema", "pvr", "inox", "bookmyshow", "gaana", "jiosaavn", "apple music", "wynk", "mx player", "alt balaji", "eros now", "hungama"]'),
('Healthcare', 'Healthcare', '💊', '#10847E', '["1mg", "pharmeasy", "netmeds", "apollo", "pharmacy", "medical", "hospital", "clinic", "doctor", "practo", "healthkart", "truemeds", "tata 1mg", "fortis", "max hospital", "medlife", "medibuddy"]'),
('Investments', 'Investments', '📈', '#00D09C', '["groww", "zerodha", "upstox", "kuvera", "paytm money", "coin", "smallcase", "mutual fund", "sip", "angel", "5paisa", "etmoney", "iifl", "motilal oswal", "hdfc securities", "icici direct", "sharekhan"]'),
('Banking', 'Banking', '🏦', '#004C8F', '["hdfc", "icici", "axis", "sbi", "kotak", "bank", "loan", "emi", "credit card", "yes bank", "idfc", "indusind", "pnb", "canara", "union bank", "rbl", "federal", "bob", "idbi", "indian bank", "hsbc", "central bank"]'),
('Personal Care', 'Personal Care', '💆', '#6A4C93', '["urban company", "salon", "spa", "barber", "beauty", "grooming", "housejoy", "yes madam", "looks salon", "lakme", "naturals"]'),
('Education', 'Education', '🎓', '#673AB7', '["byju", "unacademy", "vedantu", "coursera", "udemy", "upgrade", "school", "college", "university", "toppr", "udacity", "simplilearn", "whitehat", "great learning", "physics wallah", "allen", "aakash", "fiitjee"]'),
('Mobile', 'Mobile & Recharge', '📱', '#2A3890', '["airtel", "jio", "vodafone", "idea", "vi", "bsnl", "recharge", "prepaid", "postpaid", "mobile", "mtnl"]'),
('Fitness', 'Fitness', '💪', '#FF3278', '["cult", "gym", "fitness", "yoga", "healthifyme", "fitternity", "golds gym", "anytime fitness", "fitpass", "cure fit", "decathlon", "sports"]'),
('Insurance', 'Insurance', '🛡️', '#0066CC', '["insurance", "lic", "policy", "hdfc life", "icici pru", "sbi life", "max life", "bajaj allianz", "policybazaar", "acko", "digit", "star health", "religare", "national insurance", "new india assurance"]'),
('Salary', 'Salary', '💰', '#4CAF50', '["salary", "payroll", "wages", "stipend", "income credit"]'),
('Income', 'Other Income', '💵', '#4CAF50', '["cashback", "refund", "reimbursement", "dividend", "interest"]'),
('Others', 'Others', '📦', '#757575', '[]');

-- Create indexes for better performance (if not already created)
CREATE INDEX IF NOT EXISTS idx_banks_code ON banks(code);
CREATE INDEX IF NOT EXISTS idx_banks_is_active ON banks(is_active);
CREATE INDEX IF NOT EXISTS idx_categories_code ON categories(code);

-- Add comments for documentation
COMMENT ON TABLE banks IS 'Supported banks and financial institutions for SMS parsing';
COMMENT ON TABLE categories IS 'Transaction categories for automatic categorization';
COMMENT ON COLUMN banks.support_level IS 'full: Well tested and stable | experimental: Works but may need improvements | none: Not yet implemented';