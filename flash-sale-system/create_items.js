const http = require('http');
const items = [
    ['Samsung Galaxy S24 Ultra \u6717\u7387\u5343\u5c81\u7248', 6999, 80],
    ['OnePlus 12 \u65d7\u8230\u7248', 4599, 60],
    ['Xiaomi 14 Pro \u807a\u5f71\u65d7\u8230', 4999, 70],
    ['vivo X100 Pro \u5f71\u50cf\u65d7\u8230', 4999, 50],
    ['Apple MacBook Pro 14 M3 Pro', 12999, 40],
    ['Dell XPS 16 \u521b\u4f5c\u672c', 10999, 30],
    ['Lenovo ThinkPad X1 Carbon Gen 12', 10999, 35],
    ['iPad Air M2 \u8f7b\u8584\u5e73\u677f', 4799, 55],
    ['Microsoft Surface Pro 10', 8888, 45],
    ['Bose QuietComfort Ultra \u65e0\u7ebf\u964d\u566a', 2299, 90],
    ['Sennheiser Momentum 4 \u65e0\u7ebf\u8033\u673a', 1999, 70],
    ['Sony WF-1000XM5 \u964d\u566a\u8d5b\u5c14', 1899, 100],
    ['Jabra Elite 10 \u65e0\u7ebf\u8033\u673a', 1599, 60],
    ['iRobot Roomba j9+ \u626b\u5730\u673a\u5668\u4eba', 4999, 30],
    ['Google Nest Learning Thermostat', 1899, 40],
    ['Philips Hue \u667a\u80fd\u706f\u5149\u5957\u88c5', 1299, 50],
    ['August Smart Lock Pro \u667a\u80fd\u9501', 1599, 35],
    ['KitchenAid Artisan \u53a8\u5e08\u673a', 3999, 25],
    ['Instant Pot Duo Plus \u9ad8\u538b\u9505', 599, 80],
    ['Ninja Air Fryer Max \u7a7a\u6c14\u70b8\u9505', 799, 65],
    ['Breville Barista Pro \u5496\u5561\u673a', 3999, 30],
    ['Vitamix A3500 \u70ae\u673a\u738b\u724c', 4299, 20],
    ['Samsung Galaxy Watch 6 Classic', 2899, 55],
    ['Fitbit Charge 6 \u5065\u5eb7\u624b\u73af', 1199, 85],
    ['Garmin Fenix 7 Pro Sapphire', 5999, 25],
    ['Whoop 4.0 \u8fd0\u52a8\u624b\u73af', 1899, 45],
    ['Amazfit T-Rex 3 \u6237\u5916\u624b\u8868', 1499, 50],
    ['Apple AirPods Max \u5934\u6746\u5f0f\u964d\u566a', 3799, 40],
];
let done = 0;
items.forEach(([n,p,s]) => {
    const d = JSON.stringify({productId:1,activityName:n,flashPrice:p,totalStock:s,limitQuantity:1,qpsLimit:500,startTime:'2026-07-23T00:00:00',endTime:'2026-07-25T23:59:59'});
    const req = http.request({hostname:'localhost',port:8080,path:'/api/flash-sale/create',method:'POST',headers:{'Content-Type':'application/json; charset=utf-8','Content-Length':Buffer.byteLength(d)}},(res)=>{done++;});
    req.write(d); req.end();
});
setTimeout(() => { console.log('Created '+done+'/'+items.length); process.exit(0); }, 5000);
