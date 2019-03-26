# Telegram bot on heroku

Simple telegram bitmex price alert bot  
Checks bitmex btc price and sends a message when price reaches/passes certain value 

/start - check if bot online  
/price - get current btc price  
/add - add price to alert list  
/remove - remove price from alert list  
/watchlist - check prices on alert list

## Setup

Change application.properties in src/main/resources  

* token = your telegram bot token
* username = bot username
* heroku.website = your heroku app domain url.. https://your-app-name.herokuapp.com/  

 
