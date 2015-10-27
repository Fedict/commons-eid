# Introduction #

BeIDCardEventsManager is the most abstacted class in the EventsManager suite:
Use it to get notifications of eID cards being inserted and removed from card readers connected to the local host.

CardAndTerminalEventsManager is a lower-level abstraction that detects card terminal attaches/detaches, and card inserts/removals.

BeIDCardEventsManager internally uses an instance of CardAndTerminalEventsManager, but you can also instantiate your own and have it use that.


# Examples #

## Basic Usage ##

1. Instantiate a class that implements BeIDCardEventsListener. This instance's implementations of BeIDCardEventsListener's eIDCardInserted and eIDCardRemoved methods will get called when eID cards are inserted or removed, respectively.

2. Instantiate a BeIDCardEventsManager.

3. Call your BeIDCardEventsManager's addListener method, passing it your BeIDCardEventsListener instance

4. start() your CardAndTerminalEventsManager.

5. Your BeIDCardEventsListener instance's eIDCardInserted method will be called for any eID cards currently inserted.

6. Your BeIDCardEventsListener instance's eIDCardInserted method will be called for any eID cards inserted later, eIDCardRemoved for any eID cards removed.


## Advanced Usage ##

The Basic usage above gives you only eID card insert and removal events.
If you want to see which cardreaders are attached or detached, and/or want to see other cards being inserted and removed, you need to do a little more work.

When called without one, BeIDCardEventsManager creates a private instance of CardAndTerminalEventsManager, the lower-level class that manages card terminals and cards, but you may also instantiate your own CardAndTerminalEventsManager and pass it to BeIDCardEventsManager's constructor. It will use your instance to detect cards insertions, and at the same time you'll have a CardAndTerminalEventsManager instance: register CardTerminalEventsListener instances with it to get notifications of card terminals being attached and removed, register CardEventsListener instances to get notifications of card inserts and removals (regardless of whether they are eID cards).

1. Instantiate a CardAndTerminalEventsManager

2. Instantiate a BeIDCardEventsManager, passing the CardAndTerminalEventsManager in the constructor.

3. optionally register CardTerminalEventsListener instances with your CardAndTerminalEventsManager to receive terminal attach/detach events

4. optionally register CardEventsListener instances with your CardAndTerminalEventsManager to receive card insert/remove events

5. optionally register EiDCardEventsListener instances with your BeIDCardEventsManager to receive eID card insert/remove events.

(although you may register any number of the listeners in 3,4, and 5 above, you would need to register at least one for your classes to be useful, and if you don't register EiDCardEventsListeners, you could have done without the BeIDCardEventsManager as well)

6. start() your BeIDCardEventsManager

7. start() your CardAndTerminalEventsManager