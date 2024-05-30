package kr.co.miaps.welfare.pki;

import java.util.ArrayList;
import java.util.List;

import tradesign.certificate.CertificateInfo;
import tradesign.certificate.media.IStorage;


public class PkiSearchCert {

    private ArrayList<CertificateInfo> certs;
    CertificateInfo cert = null;


    public CertificateInfo searchCert(String _subjectDN){
        String subjectDN = _subjectDN;
        List<IStorage> storageList = IStorage.getAllStorageList();

        loop:
        for (int i = 0; i < storageList.size(); i++) {
            List<CertificateInfo> DNList = storageList.get(i).getCertInfoList();

            for (int j=0 ; j < DNList.size() ; j++){
                cert = DNList.get(j);

                if (cert.getSubjectDNStr().equalsIgnoreCase(subjectDN)){
                    break loop;
                }
            }
        }


        return cert;
    }
}
