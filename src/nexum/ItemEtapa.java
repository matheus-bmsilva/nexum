public class ItemEtapa {
    private Etapa etapa;
    private double valor;

    public ItemEtapa(Etapa etapa, double valor){
        this.etapa = etapa;
        this.valor = valor;
    }

    public void setValor(double valor){ 
        this.valor = valor; 
    }  
    
    public double getValor(){
        return valor; 
    }
  
  public void setEtapa(Etapa etapa){
        this.etapa = etapa; 
    }
  
  public Etapa getEtapa(){ 
        return etapa; 
    }
    
}
