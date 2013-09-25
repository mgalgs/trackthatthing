(defun ttt-manage-command (what)
  (compile (format "source ~/virtualenvs/ttt/bin/activate && cd /home/mgalgs/src/trackthatthing/django/ttt/ && python ./manage.py %s"
                   what)))

(defun ttt-runserver ()
  (interactive)
  (ttt-manage-command "runserver"))
