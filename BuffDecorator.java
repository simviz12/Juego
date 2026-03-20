public abstract class BuffDecorator extends EquipamientoDecorator {
    protected int duration;
    protected boolean isActive;
    
    public BuffDecorator(Personaje personaje, int duration) {
        super(personaje);
        this.duration = duration;
        this.isActive = true;
    }
    
    public void tick() {
        duration--;
        if (duration <= 0) {
            isActive = false;
        }
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public int getDuration() {
        return duration;
    }
}
