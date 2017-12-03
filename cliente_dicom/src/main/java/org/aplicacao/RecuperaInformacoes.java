package org.aplicacao;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.json.JSONObject;
import org.weasis.dicom.op.CFind;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomParam;
import org.weasis.dicom.param.DicomState;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RecuperaInformacoes {

    public static void main(String[] args) {
        new Thread(() -> {
            String adress = JOptionPane.showInputDialog("Insira o endereço do servidor", "dicomserver.co.uk");
            int porta = Integer.parseInt(JOptionPane.showInputDialog("Insira a porta", "11112"));

            while (true) {
                String[] idsDosAvaliados = solicitaIdAvaliados();
                for (String id : idsDosAvaliados) {
                    recuperaInfosDoSeverDicom(id, adress,porta);
                }

                try {
                    Thread.sleep(30 * 1000 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //método nao implementado
    private static String[] solicitaIdAvaliados() {
        return new String[]{"1"};
    }

    private static void recuperaInfosDoSeverDicom(String id, String adress, int porta) {
        DicomParam[] params = {
                new DicomParam(Tag.AccessionNumber, id),
                new DicomParam(Tag.PatientName),
                new DicomParam(Tag.PatientID),
                new DicomParam(Tag.PatientSize),
                new DicomParam(Tag.PatientWeight),
                new DicomParam(Tag.PatientSex),
                new DicomParam(Tag.PatientAge),
                new DicomParam(Tag.PatientBirthDate),
                new DicomParam(Tag.InstitutionName),
                new DicomParam(Tag.StationName),
                new DicomParam(Tag.StationAETitle),
                new DicomParam(Tag.StudyInstanceUID),
                new DicomParam(Tag.StudyID),
                new DicomParam(Tag.AccessionNumber),
                new DicomParam(Tag.StudyDate),
                new DicomParam(Tag.ProtocolName),
                new DicomParam(Tag.OperatorsName),
                new DicomParam(Tag.KVP),
                new DicomParam(Tag.SliceThickness),
                new DicomParam(Tag.TubeAngle),
                new DicomParam(Tag.ExposureStatus),
                new DicomParam(Tag.ExposureTime),
                new DicomParam(Tag.Exposure),
                new DicomParam(Tag.CalibrationDate),
                new DicomParam(Tag.Manufacturer),
                new DicomParam(Tag.NumberOfTomosynthesisSourceImages),
                new DicomParam(Tag.NumberOfSeriesRelatedInstances),
                new DicomParam(Tag.BodyPartExamined),
                new DicomParam(Tag.Modality),
                new DicomParam(Tag.AcquisitionType),
                new DicomParam(Tag.SpiralPitchFactor),
                new DicomParam(Tag.TotalCollimationWidth),
        };

        DicomNode calling = new DicomNode("PHOENIX");
        DicomNode called = new DicomNode("DICOMSERVER", adress, porta);
        DicomState state = CFind.process(calling, called, params);

        List<Attributes> items = state.getDicomRSP();
        if (items != null) {
            JSONObject jsonObject = getJsonObject(items.get(0));
            try {
                enviaDicomJsonProServer(jsonObject.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(jsonObject.toString());
        } else {
            System.out.print("Nenhum dcm encontrado com esse id");
        }

    }

    private static void enviaDicomJsonProServer(String json) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead

        try {
            HttpPost request = new HttpPost("http://ec2-54-86-129-169.compute-1.amazonaws.com/dasaHack/app_dev.php/salvarDados");
            StringEntity params = new StringEntity(json);
            request.addHeader("content-type", "application/x-www-form-urlencoded");
            request.setEntity(params);
            org.apache.http.HttpResponse response = httpClient.execute(request);

            System.out.println(response.getStatusLine());
            System.out.println(Arrays.toString(response.getAllHeaders()));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown(); //Deprecated
        }

    }


    private static JSONObject getJsonObject(Attributes item) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("patient_name", (item.getString(Tag.PatientName)== null) ? "" : item.getString(Tag.PatientName));
        jsonObject.put("patient_id", (item.getString(Tag.PatientID)== null) ? "" : item.getString(Tag.PatientID));
        jsonObject.put("patient_size", (item.getString(Tag.PatientSize)== null) ? 0 : item.getString(Tag.PatientSize));
        jsonObject.put("patient_weight", (item.getString(Tag.PatientWeight)== null) ? "" : item.getString(Tag.PatientWeight));
        jsonObject.put("patient_sex", (item.getString(Tag.PatientSex)== null) ? "" : item.getString(Tag.PatientSex));
        jsonObject.put("patient_age", (item.getString(Tag.PatientAge)== null) ? "" : item.getString(Tag.PatientAge));
        jsonObject.put("patient_bday", (item.getString(Tag.PatientBirthDate)== null) ? "" : converteDate(item.getString(Tag.PatientBirthDate)));
        jsonObject.put("institution_name", (item.getString(Tag.InstitutionName)== null) ? "" : item.getString(Tag.InstitutionName));
        jsonObject.put("station_name", (item.getString(Tag.StationName)== null) ? "" : item.getString(Tag.StationName));
        jsonObject.put("station_ae_title", (item.getString(Tag.StationAETitle)== null) ? "somaton1826" : item.getString(Tag.StationAETitle));
        jsonObject.put("study_instance_uid", (item.getString(Tag.StudyInstanceUID)== null) ? "" : item.getString(Tag.StudyInstanceUID));
        jsonObject.put("study_id", (item.getString(Tag.StudyID)== null) ? "" : item.getString(Tag.StudyID));
        jsonObject.put("accession_number", (item.getString(Tag.AccessionNumber)== null) ? "" : item.getString(Tag.AccessionNumber));
        jsonObject.put("study_datetime", (item.getString(Tag.StudyDate)== null) ? "" : converteDate(item.getString(Tag.StudyDate)));

        jsonObject.put("protocol_name", (item.getString(Tag.ProtocolName)== null) ? "" : item.getString(Tag.ProtocolName));
        jsonObject.put("operator_name", (item.getString(Tag.OperatorsName)== null) ? "" : item.getString(Tag.OperatorsName));
        jsonObject.put("kvp", (item.getString(Tag.KVP)== null) ? "" : item.getString(Tag.KVP));
        jsonObject.put("slice_thickness", (item.getString(Tag.SliceThickness)== null) ? "" : item.getString(Tag.SliceThickness));
        jsonObject.put("tube_angle", (item.getString(Tag.TubeAngle)== null) ? "" : item.getString(Tag.TubeAngle));
        jsonObject.put("exposure_current", (item.getString(Tag.ExposureStatus)== null) ? "" : item.getString(Tag.ExposureStatus));// nao sei se é essa
        jsonObject.put("exposure_time", (item.getString(Tag.ExposureTime)== null) ? "" : item.getString(Tag.ExposureTime));
        jsonObject.put("exposure", (item.getString(Tag.Exposure)== null) ? "" : item.getString(Tag.Exposure));
        jsonObject.put("dlp", "0"); //calcular
        jsonObject.put("ctii_val", "0");
        jsonObject.put("eft_dose", "0");
        jsonObject.put("ssde", "0");
        jsonObject.put("calibration_date", (item.getString(Tag.CalibrationDate)== null) ? "" : converteDate(item.getString(Tag.CalibrationDate)));
        jsonObject.put("manufacturing", (item.getString(Tag.Manufacturer)== null) ? "" : item.getString(Tag.Manufacturer));
        jsonObject.put("number_of_images", (item.getString(Tag.NumberOfTomosynthesisSourceImages)== null) ? "" : item.getString(Tag.NumberOfTomosynthesisSourceImages));
        jsonObject.put("number_of_series", (item.getString(Tag.NumberOfSeriesRelatedInstances)== null) ? "" : item.getString(Tag.NumberOfSeriesRelatedInstances));
        jsonObject.put("body_region", (item.getString(Tag.BodyPartExamined)== null) ? "" : item.getString(Tag.BodyPartExamined));
        jsonObject.put("modality_type", (item.getString(Tag.Modality)== null) ? "" : item.getString(Tag.Modality));
        jsonObject.put("acquisition_type", (item.getString(Tag.AcquisitionType)== null) ? "" : item.getString(Tag.AcquisitionType));
        jsonObject.put("pitch_factor", (item.getString(Tag.SpiralPitchFactor)== null) ? "" : item.getString(Tag.SpiralPitchFactor));
        jsonObject.put("collimation", (item.getString(Tag.TotalCollimationWidth)== null) ? "" : item.getString(Tag.TotalCollimationWidth));
        return jsonObject;
    }

    private static String converteDate(String dataDicom ) {
        String dataSql = dataDicom.substring(0,4) + '-' + dataDicom.substring(4,6) + '-' + dataDicom.substring(6,8) + " 00:00:00";
        System.out.println(dataSql);
        return dataSql;
    }
}

