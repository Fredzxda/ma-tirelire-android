function Get-ValidationPlan {
    param([string]$Suite, [string]$FullReason)
    $prefix = 'org.matirelire.android.'
    $units = @(); $devices = @()
    switch ($Suite) {
        'None' { }
        'NextDate' { $units = @('SeriesNextDateTest','SeriesRevisionTest','RecurrencesTest'); $devices = @('SeriesNextDateDeviceTest') }
        'Icons' { $units = @('CategoryIconsTest','CategoryHierarchyTest','ReferenceCatalogTest'); $devices = @('CategoryIconsDeviceTest') }
        'Forms' { $units = @('DemoViewModelTest','TransferWorkflowTest','CurrenciesTest'); $devices = @('EntryLayoutDeviceTest') }
        'Operations' { $units = @('TransactionOrderTest','TransactionSearchTest','AccountPeriodTest'); $devices = @(
            'OperationsUiDeviceTest#inlineSearchCombinesWithPeriodAndClosingPreservesCriteria',
            'OperationsUiDeviceTest#firstOutsideClickEditsOnceAndPointingNeverOpensEntry',
            'OperationsUiDeviceTest#blankOutsideTapDismissesWithoutClearingQuery') }
        'Storage' { $units = @('SnapshotCodecTest','security.*','BackupCheckpointTest'); $devices = @('VaultStorageDeviceTest') }
        'Full' { if ([string]::IsNullOrWhiteSpace($FullReason)) { throw 'Full exige -FullReason avec une justification concrete.' }; $devices = @('VaultStorageDeviceTest','OperationsUiDeviceTest','EntryLayoutDeviceTest','CategoryIconsDeviceTest','SeriesNextDateDeviceTest') }
        default { throw 'Choisir explicitement -Suite None, NextDate, Icons, Forms, Operations, Storage ou Full. Aucun test lance.' }
    }
    [pscustomobject]@{
        Suite = $Suite
        UnitTests = @($units | ForEach-Object { $prefix + $_ })
        DeviceClasses = (@($devices | ForEach-Object { $prefix + $_ }) -join ',')
        AllUnitTests = ($Suite -eq 'Full')
    }
}
