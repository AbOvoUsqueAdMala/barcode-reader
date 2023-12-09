package ru.kept.barcode.reader;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BarcodeInformation {

    Integer pageNumber;
    String barcode;

}
