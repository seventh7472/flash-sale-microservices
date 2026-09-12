import random, subprocess

brands = {
    "Apple":["iPhone","MacBook","iPad","AirPods","Apple Watch","iMac","Mac mini","Mac Pro","Vision Pro"],
    "Samsung":["Galaxy S","Galaxy Book","Galaxy Tab","Galaxy Watch","Galaxy Buds","Galaxy Z","Galaxy A"],
    "Sony":["WH-1000XM","WF-1000XM","Xperia","BRAVIA","Alpha","HT-A"],
    "Dell":["XPS","Inspiron","Alienware","Precision","Latitude"],
    "HP":["Spectre","Envy","Pavilion","EliteBook","ZBook"],
    "Lenovo":["ThinkPad X1","ThinkPad T","ThinkBook","Legion","Yoga"],
    "ASUS":["ZenBook","ROG","TUF","Vivobook","ProArt"],
    "Microsoft":["Surface Pro","Surface Laptop","Surface Book","Surface Studio"],
    "Google":["Pixel","Pixelbook","Nest Hub","Nest Thermostat"],
    "Bose":["QuietComfort","SoundLink","Bose 700","Bose Ultra"],
    "Logitech":["MX Master","G Pro","Logitech C","Logitech G"],
    "JBL":["Flip","Charge","Tune","Quantum"],
    "Sennheiser":["Momentum","HD 600","IE 600","ACCENTUM"],
    "Corsair":["K70","M65","Vengeance","Dominator"],
    "Razer":["BlackShark","DeathAdder","Blade","Kraken"],
    "Huawei":["MateBook","MatePad","Pura","FreeBuds"],
    "Xiaomi":["Xiaomi 14","RedmiBook","Xiaomi Pad","Mi Band"],
    "Canon":["EOS R","PowerShot","PIXMA","imagePROGRAF"],
    "Nikon":["Z8","Zf","D850","Coolpix"],
    "DJI":["Mavic","Mini","Air","Osmo"],
    "Sonos":["Arc","Beam","Move","Roam","Era"],
    "Meta":["Quest 3","Ray-Ban Meta"],
    "Amazon":["Echo","Fire TV","Kindle","Ring"],
    "Shure":["SM7B","MV7","SE846","AONIC"],
    "BowersWilkins":["Pi7","Px7","Zeppelin","Formation"]
}

cats = ["Smartphone","Laptop","Tablet","Smartwatch","Wireless Headphones","True Wireless Earbuds","Bluetooth Speaker","Monitor","Mechanical Keyboard","Gaming Mouse","4K Webcam","WiFi Router","USB-C Hub","Noise Cancelling Headphones","Portable SSD","Gaming Laptop","Ultralight Laptop","Android Tablet","Fitness Tracker","Wireless Earbuds","Soundbar","Conference Speaker","Action Camera","Smart Display","Robot Vacuum","Wireless Charger","Drone","Gaming Console","Streaming Device","Graphics Card"]
editions = ["2024 Edition","2025 Edition","Gen 3","Gen 4","Gen 5","V2","V3","Pro","Ultra","Max","Air","Lite","SE","Plus","Elite","Premium","Limited","Collectors Edition","White","Black","Silver","Space Gray","Deep Blue","Coral Red","Midnight Green"]

seen = set()
for i in range(0, 500, 50):
    batch = []
    for j in range(i, min(i+50, 500)):
        while True:
            brand = random.choice(list(brands.keys()))
            product = random.choice(brands[brand])
            cat = random.choice(cats)
            ed = random.choice(editions)
            name = f"{brand} {product} {ed} - {cat}"
            if name not in seen:
                seen.add(name)
                break
        price = round(random.uniform(99, 9999), 2)
        stock = random.randint(10, 200)
        batch.append(f"(1,'{name.replace(chr(39),chr(39)+chr(39))}',{price},{stock},1,500,'2026-07-23 00:00:00','2026-07-25 23:59:59',1,NOW(),NOW())")
    
    sql = "INSERT INTO seckill_activity(product_id,activity_name,flash_price,total_stock,limit_quantity,qps_limit,start_time,end_time,status,create_time,update_time) VALUES " + ",".join(batch) + ";"
    proc = subprocess.run(["docker","exec","-i","flash-mysql","mysql","-uroot","-proot","flash_sale_db"], input=sql.encode("utf-8"), capture_output=True)
    if proc.returncode == 0:
        print(f"Batch {i//50+1}/10 done ({min(i+50,500)}/500)")
    else:
        print(f"Error: {proc.stderr.decode('utf-8')[:100]}")
print("ALL 500 CREATED!")
