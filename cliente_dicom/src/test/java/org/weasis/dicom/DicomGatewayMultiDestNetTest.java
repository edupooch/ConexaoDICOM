/*******************************************************************************
 * Copyright (c) 2014 Weasis Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.dicom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.net.Status;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.weasis.dicom.op.CGetForward;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.ConnectOptions;
import org.weasis.dicom.param.DefaultAttributeEditor;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.param.ForwardDestination;
import org.weasis.dicom.param.GatewayParams;
import org.weasis.dicom.param.ProgressListener;
import org.weasis.dicom.tool.DicomGateway;

public class DicomGatewayMultiDestNetTest {

    @Test
    public void testProcess() {
        BasicConfigurator.configure();
        
        AdvancedParams params = new AdvancedParams();
        ConnectOptions connectOptions = new ConnectOptions();
        connectOptions.setConnectTimeout(3000);
        connectOptions.setAcceptTimeout(5000);
        // Concurrent DICOM operations
        connectOptions.setMaxOpsInvoked(15);
        connectOptions.setMaxOpsPerformed(15);
        params.setConnectOptions(connectOptions);

        DicomNode calling = new DicomNode("WEASIS-SCU");
        DicomNode called = new DicomNode("DICOMSERVER", "dicomserver.co.uk", 11112);
        DicomNode destination = new DicomNode("DCM4CHEE", "localhost", 11112);
        DicomNode scpNode = new DicomNode("DICOMLISTENER", "localhost", 11113);

        Map<DicomNode, List<ForwardDestination>> destinations = new HashMap<>();
        Attributes attrs = new Attributes();
        attrs.setString(Tag.PatientName, VR.PN, "Override^Patient^Name");
        attrs.setString(Tag.PatientID, VR.LO, "ModifiedPatientID");
        DefaultAttributeEditor editor = new DefaultAttributeEditor(true, attrs);
        try {
            DicomProgress progress = new DicomProgress();
            progress.addProgressListener(p -> {
                if(p.isLastFailed()) {
                    System.out.println("Failed: DICOM Status:" + p.getStatus());
                }
            });
            List<ForwardDestination> list = new ArrayList<>();
            list.add(new ForwardDestination(params, calling, destination, false, progress, editor));
            destinations.put(new DicomNode("WEASIS-SCU", "localhost", null, true), list);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        String[] acceptedCallingAETitles = destinations.keySet().stream().map(f -> f.getAet()).toArray(String[]::new);
        GatewayParams gparams = new GatewayParams(params, false, null, acceptedCallingAETitles);

        DicomGateway gateway;
        try {
            gateway = new DicomGateway(destinations);
            gateway.start(scpNode, gparams);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DicomProgress progress2 = new DicomProgress();
        progress2.addProgressListener(new ProgressListener() {

            @Override
            public void handleProgression(DicomProgress progress) {
                System.out.println("DICOM Status:" + progress.getStatus());
                System.out.println("NumberOfRemainingSuboperations:" + progress.getNumberOfRemainingSuboperations());
                System.out.println("NumberOfCompletedSuboperations:" + progress.getNumberOfCompletedSuboperations());
                System.out.println("NumberOfFailedSuboperations:" + progress.getNumberOfFailedSuboperations());
                System.out.println("NumberOfWarningSuboperations:" + progress.getNumberOfWarningSuboperations());
                if (progress.isLastFailed()) {
                    System.out.println("Last file has failed:" + progress.getProcessedFile());
                }
            }
        });

        String studyUID = "1.2.840.113619.6.374.254041414921518201393113960545126839710";

        DicomState state = CGetForward.processStudy(params, params, calling, called, scpNode, progress2, studyUID);
        // Should never happen
        Assert.assertNotNull(state);

        System.out.println("DICOM Status:" + state.getStatus());
        System.out.println(state.getMessage());

        // see org.dcm4che3.net.Status
        // See server log at http://dicomserver.co.uk/logs/
        Assert.assertThat(state.getMessage(), state.getStatus(), IsEqual.equalTo(Status.Success));
    }

}
