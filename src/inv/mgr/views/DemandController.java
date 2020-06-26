package inv.mgr.views;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import inv.mgr.model.entities.DemandaEntity;
import inv.mgr.model.entities.ProductoEntity;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import inv.mgr.model.dao.impl.DemandaIDAO;
import inv.mgr.model.dao.impl.ProductoIDAO;
import inv.mgr.utils.productioncalcs.QModel;
import inv.mgr.utils.viewsutils.Alerts;
import inv.mgr.utils.viewsutils.stringconverters.ProductoConverter;

public class DemandController extends FXController implements Initializable {
    @FXML
    private JFXButton backBtn;

    @FXML
    private JFXComboBox<ProductoEntity> productsCmb;

    @FXML
    private LineChart<?,?> qLineChart;

    @FXML
    private NumberAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private Label ddlbl;

    @FXML
    private Label qoptlbl;

    @FXML
    private Label calbl;

    @FXML
    private Label noalbl;

    @FXML
    private Label roplbl;

    @FXML
    private Label avgivlbl;

    @FXML
    private Label maxivlbl;


    public void handleBack(){
        this.mainApp.showHome();
    }

    public void handleLoad(){
        ProductoEntity selected = productsCmb.getSelectionModel().getSelectedItem();
        if(selected.getTipoDemanda().equals("Constante")){
            demandaQ(selected);
        }else{
            demandaP();
        }
    }

    private void demandaQ(ProductoEntity prod){
        //obtener demandas
        try {
            DemandaIDAO didao = new DemandaIDAO();
            DemandaEntity key = new DemandaEntity();
            key.setProductoId(prod.getProductoId());
            List<DemandaEntity> demandas = didao.findAll();
            int index = Collections.binarySearch(demandas, key, Comparator.comparingInt(DemandaEntity::getProductoId));
            System.out.println("before index");
            if(index<0){
                Alerts.simpleAlert("No hay demanda registrada para este producto", 4);
                return;
            }
            System.out.println("after index");
            key = demandas.get(index);

            if(key.getuTiempo().equals("mes")){
                key.setcTiempo(key.getcTiempo()*12);
            }
            QModel.staticInstance(key.getcTiempo(), prod.getcH(), prod.getcS(),
                    prod.getcL(), 0.1, prod.getCosto(), 250.0, null);
            Double qopt = QModel.getQoptimal(), rop = QModel.getROP(), qm = QModel.getLevelMax();

            ddlbl.setText(QModel.getDayDemand().toString());
            calbl.setText(QModel.getCostInYear().toString());
            noalbl.setText(QModel.getNOrderYear().toString());
            qoptlbl.setText(qopt.toString());
            roplbl.setText(rop.toString());
            avgivlbl.setText(QModel.getLevelAvarage().toString());
            maxivlbl.setText(qm.toString());

            drawLine(prod.getReserva(), rop, qopt, qm, QModel.getTimeBetweenOrder());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Alerts.exceptionAlert(e.getMessage(), sw.toString());
        }
    }

    private void demandaP() {
        Alerts.simpleAlert("No implementado", 2);
    }

    private void drawLine(double istock, double rop, double qopt, double qm, double t){
        qLineChart.getData().clear();
        XYChart.Series is = new XYChart.Series();
        is.getData().add(new XYChart.Data<>(0, istock));
        is.getData().add(new XYChart.Data<>(250, istock));
        is.setName("Stock de seguridad");

        XYChart.Series rorder = new XYChart.Series();
        rorder.getData().add(new XYChart.Data<>(0, rop));
        rorder.getData().add(new XYChart.Data<>(250, rop));
        rorder.setName("Punto de reorden");

        XYChart.Series qoptim = new XYChart.Series();
        qoptim.getData().add(new XYChart.Data<>(0, qopt));
        qoptim.getData().add(new XYChart.Data<>(250, qopt));
        qoptim.setName("Q Óptimo");

        XYChart.Series qmax = new XYChart.Series();
        qmax.getData().add(new XYChart.Data<>(0, qm));
        qmax.getData().add(new XYChart.Data<>(250, qm));
        qmax.setName("Q máximo");

        XYChart.Series maingraf = new XYChart.Series();

        int ct = 0;
        while (ct<=250){
            maingraf.getData().add(new XYChart.Data<>(ct, qm));
            ct+=t;
            maingraf.getData().add(new XYChart.Data<>(ct, istock));
        }
        maingraf.setName("Línea principal");

        yAxis.setUpperBound(qm+2);
        yAxis.setTickUnit(qm/5);

        qLineChart.getData().addAll(is, rorder, qoptim, qmax, maingraf);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(250);
        xAxis.setTickUnit(25);
        xAxis.setLabel("Tiempo (días)");

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(50);
        yAxis.setTickUnit(5);
        yAxis.setLabel("Unidades");
        //cargar productos en el combobox
        try {
            ProductoIDAO pidao = new ProductoIDAO();
            List<ProductoEntity> productos = pidao.findAll();
            ObservableList<ProductoEntity> prods = FXCollections.observableArrayList(productos);
            productsCmb.setItems(prods);
            productsCmb.setConverter(new ProductoConverter());
        } catch (Exception e) {
            //Forma de imprimir excepciones usando el alert
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Alerts.exceptionAlert(e.getMessage(), sw.toString());
        }

    }
}