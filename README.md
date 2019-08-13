# Telegram bot on heroku  
# @BitmexAlert_bot on telegram  

Simple telegram bitmex price alert bot  
Checks bitmex btc price and sends a message when price reaches users price on watchlist

Can use 2 apps(main and alternative) if you don't have the extra 450h dyno hours to run 24/7

* /start - check if bot online. Also tells the current xbt price  
* /add - add a price to watchlist.  
Can add multiple prices at the same time example /add 10000 11000.  
Price must be between 0 - 2*current price. Must be a natural number or end with .5
* /remove - removes the price from users watchlist.  
Can remove multiple prices at the same time example /remove 10000 11000.   
* /edit - removes the first price and adds the second.  
/edit 10000 11000 removes 10000 from the watchlist and adds 11000. 
If the wanted remove price isn't in the watchlist it still adds the number you want it to be edited to. 
* /watchlist - returns the prices on users watchlist

## Setup

Change application.properties in src/main/resources  

* token = your telegram bot token
* username = bot username
* heroku.website = your heroku app domain url.. https://your-app-name.herokuapp.com/  
* heroku.alternative.webiste = alternative heroku app domain if you decide to use 2 apps.. if not write something random in it 
* switchapp = false if you're using 1 app true if you're using 2  
* spring.data.mongodb.uri = your mongodb uri
* spring.data.mongodb.database = db name


# TODO
Tests for db  


