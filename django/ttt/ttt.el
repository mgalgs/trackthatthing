(defun ttt-manage-command (what)
  (let ((compilation-buffer-name-function (lambda (mode)
                                            (format "*ttt-%s*" what))))
    (compile (format "source ~/virtualenvs/ttt/bin/activate && cd /home/mgalgs/src/trackthatthing/django/ttt/ && python ./manage.py %s"
                     what))))

(defun ttt-runserver ()
  (interactive)
  (ttt-manage-command "runserver"))

(defun ttt-goto-setting (setting)
  (let ((settings-buffer (get-buffer "settings.py")))
    (switch-to-buffer settings-buffer)
    (goto-char 0)
    (search-forward-regexp (format "\\b%s\\b"
                                   setting))
    (move-beginning-of-line nil)
    (recenter)))

(defun ttt-goto-installed-apps ()
  (interactive)
  (ttt-goto-setting "INSTALLED_APPS"))

(require 'ansi-color)

(defun colorize-compilation-buffer ()
  (toggle-read-only)
  (ansi-color-apply-on-region (point-min) (point-max))
  (toggle-read-only))

(add-hook 'compilation-filter-hook 'colorize-compilation-buffer)
