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
import org.weasis.dicom.param.ProgressListener;

public class CGetForwardNetTest {

    @Test
    public void testProcess() {
        BasicConfigurator.configure();

        DicomProgress progress = new DicomProgress();
        progress.addProgressListener(new ProgressListener() {

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

        AdvancedParams params = new AdvancedParams();
        ConnectOptions connectOptions = new ConnectOptions();
        connectOptions.setConnectTimeout(3000);
        connectOptions.setAcceptTimeout(5000);
        params.setConnectOptions(connectOptions);

        DicomNode calling = new DicomNode("WEASIS-SCU");
        DicomNode called = new DicomNode("DICOMSERVER", "dicomserver.co.uk", 11112);
        DicomNode destination = new DicomNode("DCM4CHEE", "localhost", 11112);
        String studyUID = "1.2.840.113619.6.374.254041414921518201393113960545126839710";

        Attributes attrs = new Attributes();
        attrs.setString(Tag.PatientName, VR.PN, "Override^Patient^Name");
        attrs.setString(Tag.PatientID, VR.LO, "ModifiedPatientID");
        DefaultAttributeEditor editor = new DefaultAttributeEditor(true, attrs);


        DicomState state =
            CGetForward.processStudy(params, params, calling, called, destination, progress, studyUID, editor);
        // Should never happen
        Assert.assertNotNull(state);

        System.out.println("DICOM Status:" + state.getStatus());
        System.out.println(state.getMessage());
        System.out.println("NumberOfRemainingSuboperations:" + progress.getNumberOfRemainingSuboperations());
        System.out.println("NumberOfCompletedSuboperations:" + progress.getNumberOfCompletedSuboperations());
        System.out.println("NumberOfFailedSuboperations:" + progress.getNumberOfFailedSuboperations());
        System.out.println("NumberOfWarningSuboperations:" + progress.getNumberOfWarningSuboperations());

        // see org.dcm4che3.net.Status
        // See server log at http://dicomserver.co.uk/logs/
        Assert.assertThat(state.getMessage(), state.getStatus(), IsEqual.equalTo(Status.Success));
    }

}
