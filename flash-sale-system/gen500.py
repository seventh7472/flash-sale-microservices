import random, subprocess

brands = ["Apple","Samsung","Sony","Dell","HP","Lenovo","ASUS","LG","Google","Microsoft","Bose","JBL","Sennheiser","Logitech","Razer","Huawei","Xiaomi","Panasonic","Canon","Epson","Nikon","DJI","Corsair","SteelSeries","AMD","Intel","NVIDIA","Shure","AudioTechnica"]
types = ["Phone","Laptop","Tablet","Headphones","Earbuds","Speaker","Monitor","Keyboard","Mouse","Camera","Printer","Smartwatch","Fitness Tracker","Router","SSD","RAM","Graphics Card","Motherboard","Power Bank","Charger","Microphone","Webcam","Projector","Drone","Smart Lock","Robot Vacuum","Coffee Maker"]
prefixes = ["Pro","Ultra","Max","Air","Elite","Prime","Plus","Neo","Lite","Mini","Slim","Edge","Zen","Nova","Aura","Vibe","Flux","Nexus","Wave","Storm","Swift","Zoom"]
years = ["2024","2025","Gen 3","Gen 4","Gen 5","V2","V3","-S","-X","Pro Max","Ultra Max"]

# Delete old
subprocess.run(["docker","exec","flash-mysql","mysql","-uroot","-proot","flash_sale_db","-e","DELETE FROM seckill_activity WHERE 1=1;"], capture_output=True)

seen = set()
for i in range(0, 500, 50):
    batch = []
    for j in range(i, min(i+50, 500)):
        while True:
            name = f"{random.choice(brands)} {random.choice(types)} {random.choice(prefixes)} {random.choice(years)}"
            if name not in seen:
                seen.add(name)
                break
        price = round(random.uniform(99, 9999), 2)
        stock = random.randint(10, 200)
        batch.append(f"(1,'{name}',{price},{stock},1,500,'2026-07-23 00:00:00','2026-07-25 23:59:59',1,NOW(),NOW())")
    
    sql = "INSERT INTO seckill_activity(product_id,activity_name,flash_price,total_stock,limit_quantity,qps_limit,start_time,end_time,status,create_time,update_time) VALUES " + ",".join(batch) + ";"
    proc = subprocess.run(["docker","exec","-i","flash-mysql","mysql","-uroot","-proot","flash_sale_db"], input=sql.encode("utf-8"), capture_output=True)
    if proc.returncode == 0:
        print(f"Batch {i//50+1}/10 done ({min(i+50,500)}/500)")

print("ALL 500 CREATED!")
