Concept
=======
A US Equity/Option feed handler library written in Java

How it works
============
A given MdLibrary represents a feed (CQS, CTS, UQDF, etc) and a set of services can be registered to begin receiving real time updates of market data.  You can register multiple MdLibraries in a single app and receive as much data as your system can handle.

Supported Feeds
===============
CQS  
CTS  
UQDF  
UTDF  
NASDAQ TotalView - ITCH 5.0  
BX TotalView - ITCH 5.0  
PSX TotalView - ITCH 5.0  

Feeds In Progress
=================
ARCA  
OPRA  

Services Available
==================
NBBO - national best bid and best offer  
BBO - exchange top of book  
SALE - last trade  
STATE - marketSession, tradingState, lowerBand, upperBand  
BOOK - top 5 aggregate price levels of a US Exchange book  
IMBALANCE - opening and closing auction indicators  
