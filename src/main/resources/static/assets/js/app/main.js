define(["require", "exports", "jquery", "clipboard", "./alert", "./instant-edit", "./open-link-in-popup", "bootstrap"], function (require, exports, $, ClipboardJS, alert_1, instant_edit_1, open_link_in_popup_1) {
    "use strict";
    Object.defineProperty(exports, "__esModule", { value: true });
    var Main = /** @class */ (function () {
        function Main() {
            var _clipboard = new ClipboardJS('[data-clipboard-target]');
            _clipboard.on('success', function (e) {
                alert_1.Alert.show('Copied to your clipboard! ');
                e.clearSelection();
            });
            _clipboard.on('error', function () {
                alert_1.Alert.show('This browser doesn\'t allow copying to your clipboard, please do it manually');
            });
            new instant_edit_1.InstantEdit();
            new open_link_in_popup_1.OpenLinkInPopup();
            $('.fnd-badge[data-toggle="tooltip"]').tooltip();
            $('body').bootstrapMaterialDesign();
        }
        return Main;
    }());
    new Main();
});
