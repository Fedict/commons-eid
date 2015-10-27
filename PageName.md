# Introduction #

A BeIDCardEventsManager is the most abstacted class in the _EventsManager_ suite:
Use it to get notifications of eID cards being inserted and removed from card readers connected to the local host.


# Examples #

## Basic Usage ##

1. Instantiate a class that implements _BeIDCardEventsListener_. This instance's implementations of BeIDCardEventsListener's _eIDCardInserted_ and _eIDCardRemoved_ methods will get called when eID cards are inserted or removed, respectively.

2. Instantiate a _BeIDCardEventsManager_.

3. Call your _BeIDCardEventsManager_'s addListener method, passing it your _BeIDCardEventsListener_ instance

4. Call your _BeIDCardEventsManager_'s start() method.

5. Your _BeIDCardEventsListener_ instance's eIDCardInserted method will be called for any eID cards currently inserted.

6. Your _BeIDCardEventsListener_ instance's eIDCardInserted method will be called for any eID cards inserted later, eIDCardRemoved for any eID cards removed.


## Advanced Usage ##

The Basic usage above gives you only eID card insert and removal events.
If you want to see which cardreaders are attached or detached, and/or want to see other cards being inserted and removed, you need to do a little more work.

When called without one, _BeIDCardEventsManager_ creates a private instance of _CardAndTerminalEventsManager_, the lower-level class that manages card terminals and cards, but you may also instantiate your own _CardAndTerminalEventsManager_ and pass it to _BeIDCardEventsManager_'s constructor. It will use your instance to detect cards insertions, and at the same time you'll have a _CardAndTerminalEventsManager_ instance: register CardTerminalEventsListener instances with it to get notifications of card terminals being attached and removed, register _CardEventsListener_ instances to get notifications of card inserts and removals (regardless of whether they are eID cards).