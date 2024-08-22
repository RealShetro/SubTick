package subtick;

/*
 * Represents any level-like object which contains an {@link subtick.ITickHandler}.
 */
public interface ITickHandleable
{
  /*
   * Returns the {@link subtick.ITickHandler}
   */
  public TickHandler tickHandler();
}
