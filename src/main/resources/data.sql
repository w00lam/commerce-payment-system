INSERT INTO products (name, price, stock, description, status, category, created_at, updated_at) VALUES 
('맥북 프로 16인치', 3500000, 10, '최신형 맥북 프로', 'ON_SALE', 'ELECTRONICS', NOW(), NOW()),
('아이폰 15 프로', 1500000, 50, '최신형 아이폰', 'ON_SALE', 'ELECTRONICS', NOW(), NOW()),
('나이키 에어포스', 130000, 100, '인기 스니커즈', 'ON_SALE', 'CLOTHING', NOW(), NOW()),
('햇반 210g 24개입', 25000, 500, '맛있는 밥', 'ON_SALE', 'FOOD', NOW(), NOW()),
('설화수 윤조에센스', 120000, 0, '품절된 화장품', 'SOLD_OUT', 'BEAUTY', NOW(), NOW()),
('다이슨 에어랩', 800000, 5, '헤어 스타일러', 'ON_SALE', 'ELECTRONICS', NOW(), NOW()),
('아디다스 트레이닝복', 80000, 200, '편안한 트레이닝복', 'ON_SALE', 'CLOTHING', NOW(), NOW());

INSERT INTO membership_grades (name, min_cumulative_payment_amount, point_reward_rate, created_at, updated_at) VALUES
('NORMAL', 0, 1, NOW(), NOW()),
('VIP', 50000, 5, NOW(), NOW()),
('VVIP', 100000, 10, NOW(), NOW());

INSERT INTO plans (name, monthly_amount, description, created_at, updated_at) VALUES
('Basic Plan', 9900, '기본 요금제', NOW(), NOW()),
('Premium Plan', 19900, '프리미엄 요금제', NOW(), NOW());
